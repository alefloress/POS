from pydantic import BaseModel


class LoginIn(BaseModel):
    username: str
    password: str


class TokenPair(BaseModel):
    access_token: str
    token_type: str = "bearer"


class UserInfo(BaseModel):
    id: int
    username: str
    full_name: str
    role: str
    store_id: int
    store_name: str
