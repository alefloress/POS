from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import sysadmin
from app.routers import auth, admin, inventory, cash, sales, reports,sysadmin

app = FastAPI(title="MiCaja POS API - Version Tesis")

# CORS (ajusta orígenes según tu frontend)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],   # en prod conviene restringir
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router, prefix="/v1")
app.include_router(admin.router, prefix="/v1")
app.include_router(inventory.router, prefix="/v1")
app.include_router(cash.router, prefix="/v1")
app.include_router(sales.router, prefix="/v1")
app.include_router(reports.router, prefix="/v1")
app.include_router(sysadmin.router, prefix="/v1")


@app.get("/health")
async def health():
    return {"status": "ok"}
