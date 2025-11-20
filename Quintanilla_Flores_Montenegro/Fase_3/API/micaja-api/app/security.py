import time
import jwt
from passlib.hash import bcrypt
from typing import Optional, Dict, Any
from app.config import settings

def hash_pwd(password: str) -> str:
    return bcrypt.hash(password)


def verify_pwd(password: str, password_hash: str) -> bool:
    return bcrypt.verify(password, password_hash)


def _encode(claims: Dict[str, Any], exp_sec: int) -> str:
    now = int(time.time())
    payload = {"iat": now, "nbf": now, "exp": now + exp_sec, **claims}
    return jwt.encode(payload, settings.jwt_secret, algorithm=settings.jwt_alg)


def make_access_token(*, sub: str, uid: int, role: str, store_id: int) -> str:
    base = {"sub": sub, "uid": uid, "role": role, "store_id": store_id}
    return _encode(base, settings.jwt_access_minutes * 60)


def decode_token(token: str) -> Dict[str, Any]:
    return jwt.decode(token, settings.jwt_secret, algorithms=[settings.jwt_alg])
