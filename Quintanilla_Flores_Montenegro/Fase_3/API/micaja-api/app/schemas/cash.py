from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class CashOpenIn(BaseModel):
    register_id: int
    opening_amount: float


class CashSessionOut(BaseModel):
    id: int
    register_id: int
    user_id: int
    opened_at: datetime
    closed_at: Optional[datetime] = None
    opening_amount: float
    declared_amount: Optional[float] = None
    closing_amount: Optional[float] = None
    status: str
    note: Optional[str] = None


class CashCloseIn(BaseModel):
    session_id: int
    declared_amount: float
    closing_amount: float
    note: Optional[str] = None
