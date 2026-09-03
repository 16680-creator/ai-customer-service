#!/usr/bin/env python3
"""
GitHub/Gitee 泄露密钥扫描器
用途：搜索公开代码库中泄露的 API Key、Token、URL 等敏感信息
注意：仅用于安全研究和漏洞赏猎，发现泄露后应通知相关方修复
"""

import requests
import re
import json
import time
import argparse
from dataclasses import dataclass
from typing import List, Optional
from datetime import datetime


@dataclass
class Finding:
    source: str          # github / gitee
    file_url: str
    repo: str
    match_type: str      # api_key / token / url / password
    match_value: str     # 脱敏后的值
    line_number: int
    snippet: str         # 代码片段（脱敏）


# 泄露模式正则（脱敏匹配，不记录完整值）
PATTERNS = {
    "aws_access_key": {
        "regex": r"AKIA[0-9A-Z]{16}",
        "description": "AWS Access Key"
    },
    "aws_secret_key": {
        "regex": r"(?:aws_secret_access_key|secret_key)\s*[:=]\s*['\"]?([A-Za-z0-9/+=]{40})",
        "description": "AWS Secret Key"
    },
    "github_token": {
        "regex": r"ghp_[A-Za-z0-9]{36}",
        "description": "GitHub Personal Access Token"
    },
    "github_oauth": {
        "regex": r"gho_[A-Za-z0-9]{36}",
        "description": "GitHub OAuth Token"
    },
    "gitlab_token": {
        "regex": r"glpat-[A-Za-z0-9\-_]{20,}",
        "description": "GitLab Personal Access Token"
    },
    "slack_token": {
        "regex": r"xoxb-[0-9]{10,}-[A-Za-z0-9]{24,}",
        "description": "Slack Bot Token"
    },
    "slack_webhook": {
        "regex": r"https://hooks\.slack\.com/services/T[A-Z0-9]{8,}/B[A-Z0-9]{8,}/[A-Za-z0-9]{24,}",
        "description": "Slack Webhook URL"
    },
    "openai_key": {
        "regex": r"sk-[A-Za-z0-9]{48}",
        "description": "OpenAI API Key"
    },
    "anthropic_key": {
        "regex": r"sk-ant-[A-Za-z0-9\-]{40,}",
        "description": "Anthropic API Key"
    },
    "stripe_key": {
        "regex": r"(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{24,}",
        "description": "Stripe API Key"
    },
    "private_key_header": {
        "regex": r"-----BEGIN (?:RSA |EC )?PRIVATE KEY-----",
        "description": "Private Key Header"
    },
    "jdbc_url": {
        "regex": r"jdbc:(?:mysql|postgresql|oracle|sqlserver)://[^\s'\"]+",
        "description": "JDBC Connection URL"
    },
    "redis_url": {
        "regex": r"redis://[^\s'\"]+",
        "description": "Redis Connection URL"
    },
    "generic_secret": {
        "regex": r"(?:secret|password|api_key|apikey|api_secret|token)\s*[:=]\s*['\"]([^\s'\"]{8,})",
        "description": "Generic Secret Value"
    },
}


def mask_value(value: str, show_chars: int = 4) -> str:
    """脱敏显示值"""
    if len(value) <= show_chars * 2:
        return value[:2] + "***"
    return value[:show_chars] + "***" + value[-show_chars:]


def mask_snippet(line: str, match_start: int, match_end: int, context: int = 20) -> str:
    """脱敏代码片段"""
    start = max(0, match_start - context)
    end = min(len(line), match_end + context)
    snippet = line[start:end].strip()
    # 替换匹配值为脱敏版本
    original = line[match_start:match_end]
    return snippet.replace(original, "***REDACTED***")


class GitHubScanner:
    """GitHub 代码搜索"""
    
    BASE_URL = "https://api.github.com"
    
    def __init__(self, token: Optional[str] = None):
        self.session = requests.Session()
        if token:
            self.session.headers["Authorization"] = f"token {token}"
        self.session.headers["Accept"] = "application/vnd.github.v3.text-match+json"
    
    def search_code(self, query: str, per_page: int = 30) -> List[dict]:
        """搜索代码"""
        url = f"{self.BASE_URL}/search/code"
        params = {
            "q": query,
            "per_page": per_page,
            "sort": "indexed",
            "order": "desc"
        }
        
        try:
            resp = self.session.get(url, params=params, timeout=30)
            if resp.status_code == 403:
                print("[!] GitHub API rate limit hit, waiting 60s...")
                time.sleep(60)
                resp = self.session.get(url, params=params, timeout=30)
            resp.raise_for_status()
            return resp.json().get("items", [])
        except requests.RequestException as e:
            print(f"[!] GitHub search error: {e}")
            return []
    
    def scan(self, patterns: dict, max_results: int = 100) -> List[Finding]:
        """扫描所有模式"""
        findings = []
        
        for name, config in patterns.items():
            print(f"[*] GitHub: Searching {config['description']}...")
            
            # 使用关键特征搜索（避免直接搜完整正则）
            search_terms = self._get_search_terms(name)
            for term in search_terms:
                items = self.search_code(f'"{term}"', per_page=10)
                
                for item in items:
                    # 获取文件内容验证
                    file_findings = self._check_file(item, name, config["regex"])
                    findings.extend(file_findings)
                    
                    if len(findings) >= max_results:
                        return findings
                
                time.sleep(2)  # 避免触发限流
        
        return findings
    
    def _get_search_terms(self, pattern_name: str) -> List[str]:
        """获取搜索关键词"""
        terms = {
            "aws_access_key": ["AKIA", "aws_access_key_id"],
            "github_token": ["ghp_"],
            "openai_key": ["sk-", "OPENAI_API_KEY"],
            "slack_token": ["xoxb-"],
            "private_key_header": ["BEGIN PRIVATE KEY"],
            "jdbc_url": ["jdbc:mysql://", "jdbc:postgresql://"],
            "redis_url": ["redis://"],
            "generic_secret": ["api_key=", "secret=", "token="],
        }
        return terms.get(pattern_name, [])
    
    def _check_file(self, item: dict, pattern_name: str, regex: str) -> List[Finding]:
        """检查文件内容"""
        findings = []
        repo = item.get("repository", {}).get("full_name", "unknown")
        file_url = item.get("html_url", "")
        
        try:
            # 获取原始文件内容
            raw_url = item.get("download_url")
            if not raw_url:
                return findings
            
            resp = self.session.get(raw_url, timeout=10)
            if resp.status_code != 200:
                return findings
            
            content = resp.text
            lines = content.split("\n")
            
            for i, line in enumerate(lines, 1):
                match = re.search(regex, line)
                if match:
                    findings.append(Finding(
                        source="github",
                        file_url=file_url,
                        repo=repo,
                        match_type=pattern_name,
                        match_value=mask_value(match.group()),
                        line_number=i,
                        snippet=mask_snippet(line, match.start(), match.end())
                    ))
        except Exception as e:
            pass  # 静默处理错误
        
        return findings


class GiteeScanner:
    """Gitee 代码搜索"""
    
    BASE_URL = "https://gitee.com/api/v5"
    
    def __init__(self, token: Optional[str] = None):
        self.session = requests.Session()
        if token:
            self.session.params["access_token"] = token
    
    def search_code(self, query: str, page: int = 1, per_page: int = 20) -> List[dict]:
        """搜索代码"""
        url = f"{self.BASE_URL}/search/code"
        params = {
            "q": query,
            "page": page,
            "per_page": per_page,
            "order": "desc"
        }
        
        try:
            resp = self.session.get(url, params=params, timeout=30)
            resp.raise_for_status()
            data = resp.json()
            return data.get("items", []) if isinstance(data, dict) else data
        except requests.RequestException as e:
            print(f"[!] Gitee search error: {e}")
            return []
    
    def scan(self, patterns: dict, max_results: int = 100) -> List[Finding]:
        """扫描所有模式"""
        findings = []
        
        for name, config in patterns.items():
            print(f"[*] Gitee: Searching {config['description']}...")
            
            search_terms = self._get_search_terms(name)
            for term in search_terms:
                items = self.search_code(term)
                
                for item in items:
                    file_findings = self._check_file(item, name, config["regex"])
                    findings.extend(file_findings)
                    
                    if len(findings) >= max_results:
                        return findings
                
                time.sleep(2)
        
        return findings
    
    def _get_search_terms(self, pattern_name: str) -> List[str]:
        """获取搜索关键词"""
        terms = {
            "aws_access_key": ["AKIA"],
            "github_token": ["ghp_"],
            "openai_key": ["sk-"],
            "private_key_header": ["BEGIN PRIVATE KEY"],
            "jdbc_url": ["jdbc:mysql://", "jdbc:postgresql://"],
            "redis_url": ["redis://"],
        }
        return terms.get(pattern_name, [])
    
    def _check_file(self, item: dict, pattern_name: str, regex: str) -> List[Finding]:
        """检查文件内容"""
        findings = []
        repo = item.get("repository", {}).get("full_name", "unknown")
        file_url = item.get("html_url", "")
        
        try:
            raw_url = item.get("raw_url") or item.get("download_url")
            if not raw_url:
                return findings
            
            resp = self.session.get(raw_url, timeout=10)
            if resp.status_code != 200:
                return findings
            
            content = resp.text
            lines = content.split("\n")
            
            for i, line in enumerate(lines, 1):
                match = re.search(regex, line)
                if match:
                    findings.append(Finding(
                        source="gitee",
                        file_url=file_url,
                        repo=repo,
                        match_type=pattern_name,
                        match_value=mask_value(match.group()),
                        line_number=i,
                        snippet=mask_snippet(line, match.start(), match.end())
                    ))
        except Exception:
            pass
        
        return findings


def generate_report(findings: List[Finding]) -> str:
    """生成扫描报告"""
    report = []
    report.append("=" * 80)
    report.append("泄露密钥扫描报告")
    report.append(f"扫描时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    report.append(f"发现数量: {len(findings)}")
    report.append("=" * 80)
    
    # 按类型分组
    by_type = {}
    for f in findings:
        by_type.setdefault(f.match_type, []).append(f)
    
    for match_type, items in by_type.items():
        report.append(f"\n## {match_type} ({len(items)} 个)")
        report.append("-" * 40)
        
        for i, f in enumerate(items, 1):
            report.append(f"\n  [{i}] {f.source.upper()}")
            report.append(f"      仓库: {f.repo}")
            report.append(f"      文件: {f.file_url}")
            report.append(f"      行号: {f.line_number}")
            report.append(f"      值:   {f.match_value}")
            report.append(f"      片段: {f.snippet}")
    
    report.append("\n" + "=" * 80)
    report.append("建议: 发现泄露后应立即通知仓库所有者轮换凭据")
    report.append("=" * 80)
    
    return "\n".join(report)


def main():
    parser = argparse.ArgumentParser(description="GitHub/Gitee 泄露密钥扫描器")
    parser.add_argument("--github-token", help="GitHub API Token (可选，提高速率限制)")
    parser.add_argument("--gitee-token", help="Gitee API Token (可选)")
    parser.add_argument("--source", choices=["github", "gitee", "all"], default="all",
                       help="扫描来源 (默认: all)")
    parser.add_argument("--max-results", type=int, default=50,
                       help="最大发现数量 (默认: 50)")
    parser.add_argument("--output", "-o", help="输出报告文件路径")
    parser.add_argument("--json", action="store_true", help="输出 JSON 格式")
    
    args = parser.parse_args()
    
    findings = []
    
    # GitHub 扫描
    if args.source in ("github", "all"):
        print("\n[+] Starting GitHub scan...")
        github_scanner = GitHubScanner(token=args.github_token)
        findings.extend(github_scanner.scan(PATTERNS, args.max_results))
    
    # Gitee 扫描
    if args.source in ("gitee", "all"):
        print("\n[+] Starting Gitee scan...")
        gitee_scanner = GiteeScanner(token=args.gitee_token)
        findings.extend(gitee_scanner.scan(PATTERNS, args.max_results))
    
    # 输出结果
    print(f"\n[+] Scan complete. Found {len(findings)} potential leaks.\n")
    
    if args.json:
        output = json.dumps([{
            "source": f.source,
            "repo": f.repo,
            "file_url": f.file_url,
            "line_number": f.line_number,
            "match_type": f.match_type,
            "match_value": f.match_value,
            "snippet": f.snippet
        } for f in findings], indent=2, ensure_ascii=False)
    else:
        output = generate_report(findings)
    
    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(output)
        print(f"[+] Report saved to {args.output}")
    else:
        print(output)


if __name__ == "__main__":
    main()
