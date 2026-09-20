import logging

from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import settings
from app.routers import (
    admin,
    auth,
    customer,
    dashboard,
    documents,
    drones,
    flights,
    invoices,
    maintenance,
    notifications,
    search,
    service_history,
    tickets,
    warranty,
)

logging.basicConfig(level=logging.INFO)

app = FastAPI(
    title="NeoSky Service & Support API",
    description="Backend API for the NeoSky Service & Support drone customer-service app.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    detail = exc.detail
    if isinstance(detail, dict) and "error" in detail:
        body = detail
    else:
        body = {"error": {"code": "ERROR", "message": str(detail)}}
    return JSONResponse(status_code=exc.status_code, content=body)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    fields: dict[str, str] = {}
    for err in exc.errors():
        loc = err.get("loc", ())
        field_name = ".".join(str(part) for part in loc if part not in ("body", "query", "path"))
        fields[field_name or "__root__"] = err.get("msg", "invalid value")
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "error": {
                "code": "VALIDATION_ERROR",
                "message": "Request validation failed",
                "fields": fields,
            }
        },
    )


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    logging.getLogger("neosky.errors").exception("Unhandled error on %s %s", request.method, request.url)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"error": {"code": "INTERNAL_ERROR", "message": "An unexpected error occurred"}},
    )


app.include_router(auth.router)
app.include_router(customer.router)
app.include_router(dashboard.router)
app.include_router(drones.router)
app.include_router(tickets.router)
app.include_router(flights.router)
app.include_router(warranty.router)
app.include_router(maintenance.router)
app.include_router(invoices.router)
app.include_router(service_history.router)
app.include_router(documents.router)
app.include_router(notifications.router)
app.include_router(search.router)
app.include_router(admin.router)


@app.get("/health", tags=["health"])
def health_check():
    return {"status": "ok"}
