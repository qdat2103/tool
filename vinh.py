import requests
import json
import os
import sys
from sys import platform
from datetime import datetime
from time import sleep, strftime

# Kiểm tra và cài đặt thư viện nếu chưa có
try:
    from pystyle import Add, Center, Anime, Colors, Colorate, Write, System
except ImportError:
    os.system('pip install pystyle requests bs4 colorama beautifulsoup4 Anime webdriver_manager selenium mechanize')

# Định nghĩa màu sắc
den = "\033[1;90m"
luc = "\033[1;32m"
trang = "\033[1;37m"
red = "\033[1;31m"
vang = "\033[1;33m"
tim = "\033[1;35m"
lamd = "\033[1;34m"
lam = "\033[1;36m"
purple = "\033[35m"  # Đã sửa lại màu purple
hong = "\033[1;95m"
thanh_xau = trang + '~' + red + '[' + vang + 'C25' + red + '] ' + trang + '➩ ' + luc
thanh_dep = trang + '~' + red + '[' + luc + 'C25' + red + '] ' + trang + '➩ ' + luc

# Banner hiển thị thông tin
banners = f"""
██╗░░░██╗██╗███╗░░██╗██╗░░██╗
██║░░░██║██║████╗░██║██║░░██║
╚██╗░██╔╝██║██╔██╗██║███████║
░╚████╔╝░██║██║╚████║██╔══██║
░░╚██╔╝░░██║██║░╚███║██║░░██║
░░░╚═╝░░░╚═╝╚═╝░░╚══╝╚═╝░░╚═╝

████████╗░█████╗░░█████╗░██╗░░░░░
╚══██╔══╝██╔══██╗██╔══██╗██║░░░░░
░░░██║░░░██║░░██║██║░░██║██║░░░░░
░░░██║░░░██║░░██║██║░░██║██║░░░░░
░░░██║░░░╚█████╔╝╚█████╔╝███████╗
░░░╚═╝░░░░╚════╝░░╚════╝░╚══════╝"""

thongtin = f"""tool gốc của c25 được chỉnh sửa lại nhé 
by: Võ Thanh Vinh"""

# Hàm hiển thị banner
def banner():
    print(Colorate.Horizontal(Colors.green_to_yellow, banners))
    print(Colorate.Horizontal(Colors.blue_to_purple, thongtin))

# Gọi hàm banner để hiển thị
banner()

#Định nghĩa màu sắc
lam = "\033[1;36m"
hong = "\033[1;95m"
luc = "\033[1;32m"
trang = "\033[1;37m"
do = "\033[1;31m"
vang = "\033[1;33m"  # Định nghĩa màu vàng
thanh_xau = "│"  # Định nghĩa thanh xấu

# In ra menu
print(f"""{luc}┏━━━━━━━━━━━━━━━━━━━━━━━┓
{vang}┃ {vang}Tool TDS & TTC {vang} ┃
{lam}┗━━━━━━━━━━━━━━━━━━━━━━━┛ """)
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}1.1{lam}] {lam}Tool Cày Xu TDS Tiktok')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}1.2{lam}] {lam}Tool Cày Xu TDS Instagram')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}1.3{lam}] {lam}Tool Golike TikTok [ADR]')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}1.4{lam}] {lam}Tool Golike Instagram')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}1.5{lam}] {lam}Tool Cày Xu TDS Facebook')
print(f'{luc}{lam}────────────────────────────────────────────────────────')
print(f"""{luc}┏━━━━━━━━━━━━━━━━━━━━━━━┓
{vang}┃ {vang}Tool Nghịch {vang}┃
{lam}┗━━━━━━━━━━━━━━━━━━━━━━━┛""")
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}2.1{lam}] {lam}Tool Buff Share Ảo Cookie ')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}2.2{lam}] {lam}Tool Get Token Facebook 16 Loại')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}2.3{lam}] {lam}Tool Lấy ID Bài Viết, ID Facebook')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}2.4{lam}] {lam}Tool CMT Bài Viết Dạo Facebook[bảo trì]')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}2.5{lam}] {lam}Tool Get Cookie Facebook Bằng TK MK')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}2.6{lam}] {lam}Tool Spam Tin Nhắn, War Messenger')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}2.7{lam}] {lam}Tool Nuôi Acc FB')
print(f'{luc}{lam}────────────────────────────────────────────────────────')
print(f"""{luc}┏━━━━━━━━━━━━━━━━━━━━━━━┓
{vang}┃ {vang}Tool Tiện Ích {vang}┃
{lam}┗━━━━━━━━━━━━━━━━━━━━━━━┛""")
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}3.1{lam}] {lam}Tool Doss Web + Doss IP')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}3.2{lam}] {lam}Tool Get Proxy')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}3.3{lam}] {lam}Tool Lọc Proxy')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}3.4{lam}] {lam}Tool Scan Mail Ảo Lấy Mã')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}3.5{lam}] {lam}Tool Spam SĐT')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}3.6{lam}] {lam}Tool Buff View Tiktok [PC]')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}3.7{lam}] {lam}Tool Reg Nick FB')
print(f'{thanh_xau}{luc}Nhập {luc}[{vang}3.8{lam}] {lam}Tool Reg Acc Garena')
print(f'───────────────────────────────────────────────────────────────────')

# Nhập số
chon = input(f'{thanh_xau}{do}Nhập Số: {vang}')

# Xử lý lựa chọn người dùng
try:
    if chon == '1.1':
        exec(requests.get('https://taokey567.c25tool.net/htacces/1.1.php').text)
    elif chon == '1.2':
        exec(requests.get('https://taokey567.c25tool.net/htacces/1.2.php').text)
    elif chon == '1.3':
        exec(requests.get('https://taokey567.c25tool.net/htacces/1.3.php').text)
    elif chon == '1.4':
        exec(requests.get('https://taokey567.c25tool.net/htacces/1.4.php').text)
    elif chon == '1.5':
        exec(requests.get('https://taokey567.c25tool.net/htacces/1.5.php').text)
    elif chon == '2.1':
        exec(requests.get('https://taokey567.c25tool.net/htacces/2.1.php').text)
    elif chon == '2.2':
        exec(requests.get('https://taokey567.c25tool.net/htacces/2.2.php').text)
    elif chon == '2.3':
        exec(requests.get('https://taokey567.c25tool.net/htacces/2.3.php').text)
    elif chon == '2.4':
        exec(requests.get('https://taokey567.c25tool.net/htacces/2.4.php').text)
    elif chon == '2.5':
        exec(requests.get('https://taokey567.c25tool.net/htacces/2.5.php').text)
    elif chon == '2.6':
        exec(requests.get('https://taokey567.c25tool.net/htacces/2.6.php').text)
    elif chon == '2.7':
        exec(requests.get('https://taokey567.c25tool.net/htacces/2.7.php').text)
    elif chon == '2.8':
        exec(requests.get('https://taokey567.c25tool.net/htacces/2.8.php').text)
    elif chon == '3.1':
        exec(requests.get('https://taokey567.c25tool.net/htacces/3.1.php').text)
    elif chon == '3.2':
        exec(requests.get('https://taokey567.c25tool.net/htacces/3.2.php').text)
    elif chon == '3.3':
        exec(requests.get('https://taokey567.c25tool.net/htacces/3.3.php').text)
    elif chon == '3.4':
        exec(requests.get('https://taokey567.c25tool.net/htacces/3.4.php').text)
    elif chon == '3.5':
        exec(requests.get('https://taokey567.c25tool.net/htacces/3.5.php').text)
    elif chon == '3.6':
        exec(requests.get('https://taokey567.c25tool.net/htacces/3.6.php').text)
    elif chon == '3.7':
        exec(requests.get('https://taokey567.c25tool.net/htacces/3.7.php').text)
    elif chon == '3.8':
        exec(requests.get('https://taokey567.c25tool.net/config-asset/3.8.php').text)
    else:
        print(f"{do}Không có lựa chọn này.")
except Exception as e:
    print(f"{do}Đã có lỗi xảy ra: {e}")
