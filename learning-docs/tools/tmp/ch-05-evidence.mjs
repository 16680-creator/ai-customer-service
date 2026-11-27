import { readFileSync } from 'node:fs';
const show = (f, re) => {
  console.log('--- ' + f);
  readFileSync('D:/Projects/Persion/ai-customer-service/' + f, 'utf8').split(/\r?\n/)
    .forEach((s, i) => { if (re.test(s)) console.log(String(i + 1).padStart(4) + ': ' + s.trim()); });
};
show('ai-cs-message/src/main/java/com/aics/message/service/impl/ModelUsageServiceImpl.java',
  /selectList|records\.size|mapToLong|filter\(|reduce\(|内存聚合/);
show('ai-cs-chat/src/main/java/com/aics/chat/observability/QuotaService.java',
  /FeignClient|getStats|windowStart|catch|isExceeded/);
