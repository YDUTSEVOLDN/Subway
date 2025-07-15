from flask import Blueprint

bp = Blueprint('api', __name__)

# 导入路由模块
from app.api import metro