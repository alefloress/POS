# app/config.py

from pydantic_settings import BaseSettings 


class Settings(BaseSettings):
    env: str = "dev"

    db_host: str
    db_port: int = 5432
    db_name: str
    db_user: str
    db_password: str

    jwt_secret: str
    jwt_alg: str = "HS256"
    jwt_access_minutes: int = 640

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
