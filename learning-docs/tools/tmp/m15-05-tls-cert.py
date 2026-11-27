# 生成仅本机实验用的自签证书（不是生产证书，勿用于任何真实站点）
from datetime import datetime, timedelta, timezone
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa

key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
name = x509.Name([
    x509.NameAttribute(NameOID.COUNTRY_NAME, "CN"),
    x509.NameAttribute(NameOID.ORGANIZATION_NAME, "ai-cs learning-docs"),
    x509.NameAttribute(NameOID.COMMON_NAME, "localhost"),
])
now = datetime.now(timezone.utc)
cert = (x509.CertificateBuilder()
        .subject_name(name).issuer_name(name)
        .public_key(key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(now).not_valid_after(now + timedelta(days=30))
        .add_extension(x509.SubjectAlternativeName([
            x509.DNSName("localhost"), x509.IPAddress(__import__("ipaddress").ip_address("127.0.0.1"))]), critical=False)
        .sign(key, hashes.SHA256()))
open("m15-05-localhost-cert.pem","wb").write(cert.public_bytes(serialization.Encoding.PEM))
open("m15-05-localhost-key.pem","wb").write(key.private_bytes(
    serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
print("cert subject:", cert.subject.rfc4514_string())
print("cert sha256 :", cert.fingerprint(hashes.SHA256()).hex())
print("cert sigalg :", cert.signature_algorithm_oid._name)
print("valid       :", cert.not_valid_before_utc.date(), "->", cert.not_valid_after_utc.date())
