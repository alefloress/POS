from fastapi import APIRouter, Depends, Query
from datetime import date
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_session
from app.deps import require_admin
from app.schemas.reports import DashboardOut

router = APIRouter(prefix="/reports", tags=["reports"])


@router.get("/dashboard", response_model=DashboardOut)
async def dashboard(
    claims = Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    # TODO: calcular KPIs + tablas para dashboard
    raise NotImplementedError


@router.get("/sales-range")
async def sales_range(
    date_from: date,
    date_to: date,
    claims = Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    # TODO: sumar ventas entre fechas, devolver lista
    raise NotImplementedError


@router.get("/cash-movements")
async def cash_movements(
    date_from: date = Query(...),
    date_to: date = Query(...),
    claims = Depends(require_admin),
    db: AsyncSession = Depends(get_session),
):
    # TODO: listar aperturas y cierres de caja
    raise NotImplementedError
