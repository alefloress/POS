# app/deps.py
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from app.security import decode_token

bearer = HTTPBearer()

async def get_current(
    creds: HTTPAuthorizationCredentials = Depends(bearer),
) -> dict:
    if not creds:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="NO_AUTH_HEADER",
        )

    try:
        claims = decode_token(creds.credentials)
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="TOKEN_INVALIDO",
        )

    # Validaciones mínimas que tú quieras
    if "uid" not in claims or "role" not in claims:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="TOKEN_INCOMPLETO",
        )

    return claims

async def require_admin(claims: dict = Depends(get_current)) -> dict:
    """Permite ADMIN y SYSADMIN (SYSADMIN como superusuario)."""
    role = (claims.get("role") or "").upper()
    if role not in ("ADMIN", "SYSADMIN"):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="SOLO_ADMIN",
        )
    return claims

async def require_sysadmin(claims: dict = Depends(get_current)) -> dict:
    """Sólo permite SYSADMIN."""
    role = (claims.get("role") or "").upper()
    if role != "SYSADMIN":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="SOLO_SYSADMIN",
        )
    return claims
