from pydantic import BaseModel
from typing import List, Optional
from datetime import date, datetime


class DashboardKPIs(BaseModel):
    sales_today: float
    sales_7d: float
    sales_30d: float
    products_in_inventory: int


class ProductSimple(BaseModel):
    id: int
    code: str
    name: str
    stock: int
    reorder_threshold: int
    expiry_date: Optional[date] = None


class RecentSale(BaseModel):
    id: int
    created_at: datetime
    total: float
    payment_method: str
    username: str


class DashboardOut(BaseModel):
    kpis: DashboardKPIs
    low_stock: List[ProductSimple]
    expiring_soon: List[ProductSimple]
    recent_sales: List[RecentSale]
