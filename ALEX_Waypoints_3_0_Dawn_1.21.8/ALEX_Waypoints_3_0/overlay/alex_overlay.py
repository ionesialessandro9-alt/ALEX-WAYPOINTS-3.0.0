import sys,json,math
from pathlib import Path
import win32gui,mss
from PySide6.QtCore import Qt,QTimer,QRectF
from PySide6.QtGui import QImage,QPixmap,QPainter,QPen,QColor,QFont
from PySide6.QtWidgets import QApplication,QMainWindow,QWidget,QVBoxLayout,QHBoxLayout,QLabel,QPushButton,QComboBox

PROFILES=Path.home()/"AppData/Roaming/.dawn/profiles"
def live_file():
    found=list(PROFILES.glob("**/.minecraft/config/alex-waypoints-live.json")) if PROFILES.exists() else []
    direct=Path.home()/"AppData/Roaming/.minecraft/config/alex-waypoints-live.json"
    return direct if direct.exists() else (found[0] if found else None)
def windows():
    a=[]
    def cb(h,_):
        title=win32gui.GetWindowText(h)
        if win32gui.IsWindowVisible(h) and title and any(x in title.lower() for x in ("minecraft","feather","dawn")):
            try:
                l,t,r,b=win32gui.GetClientRect(h)
                if r>300 and b>200:a.append((h,title))
            except:pass
    win32gui.EnumWindows(cb,None);return a

class View(QWidget):
    def __init__(self):super().__init__();self.setMinimumSize(640,360);self.frame=None;self.data=None
    def update_view(self,im,d):self.frame=im;self.data=d;self.update()
    def paintEvent(self,e):
        p=QPainter(self);p.fillRect(self.rect(),QColor(12,12,16))
        if not self.frame:p.setPen(Qt.white);p.drawText(self.rect(),Qt.AlignCenter,"Apri Minecraft 1.21.8");return
        pix=QPixmap.fromImage(self.frame).scaled(self.size(),Qt.KeepAspectRatio,Qt.SmoothTransformation)
        ox=(self.width()-pix.width())/2;oy=(self.height()-pix.height())/2;p.drawPixmap(int(ox),int(oy),pix)
        d=self.data
        if not d:return
        dx=d["x"]-d["playerX"];dy=d["y"]-d["playerY"];dz=d["z"]-d["playerZ"]
        yaw=math.radians(d.get("yaw",0));pitch=math.radians(d.get("pitch",0))
        fwd=(-math.sin(yaw)*math.cos(pitch),-math.sin(pitch),math.cos(yaw)*math.cos(pitch))
        right=(math.cos(yaw),0,math.sin(yaw));up=(math.sin(yaw)*math.sin(pitch),math.cos(pitch),-math.cos(yaw)*math.sin(pitch))
        dist=math.sqrt(dx*dx+dy*dy+dz*dz)
        if dist<.01:return
        v=(dx/dist,dy/dist,dz/dist);z=sum(v[i]*fwd[i] for i in range(3))
        if z<=.02:return
        cx=sum(v[i]*right[i] for i in range(3));cy=sum(v[i]*up[i] for i in range(3));f=1/math.tan(math.radians(70)/2)
        x=ox+pix.width()*(.5+.5*f*cx/z);y=oy+pix.height()*(.5-.5*f*cy/z)
        if not(ox-20<x<ox+pix.width()+20 and oy-20<y<oy+pix.height()+20):return
        p.setRenderHint(QPainter.Antialiasing);p.setPen(QPen(QColor(245,25,55),3));p.drawLine(int(x),int(max(oy,y-100)),int(x),int(min(oy+pix.height(),y+42)));p.drawEllipse(int(x-12),int(y-12),24,24)
        label=f'{d["name"]} ({dist:.0f}m)';p.setFont(QFont("Segoe UI",11,QFont.Bold));w=p.fontMetrics().horizontalAdvance(label)+20;r=QRectF(x-w/2,y-48,w,27)
        p.setBrush(QColor(10,10,15,225));p.drawRoundedRect(r,7,7);p.setPen(QPen(QColor(245,25,55),2));p.drawRoundedRect(r,7,7);p.setPen(Qt.white);p.drawText(r,Qt.AlignCenter,label)

class App(QMainWindow):
    def __init__(self):
        super().__init__();self.setWindowTitle("ALEX WAYPOINTS 3.0");self.resize(1100,720);self.view=View();self.combo=QComboBox();self.status=QLabel("ALEX / PRIVATE WAYPOINTS");self.status.setStyleSheet("color:#ff4058;font-weight:bold;padding:8px")
        btn=QPushButton("Aggiorna finestre");btn.clicked.connect(self.scan)
        row=QHBoxLayout();row.addWidget(self.status,1);row.addWidget(QLabel("Minecraft:"));row.addWidget(self.combo);row.addWidget(btn)
        col=QVBoxLayout();col.addLayout(row);col.addWidget(self.view,1);w=QWidget();w.setLayout(col);self.setCentralWidget(w);self.setStyleSheet("QMainWindow,QWidget{background:#111116;color:#eee;font-family:'Segoe UI'} QPushButton,QComboBox{background:#292932;padding:7px;border:1px solid #555;border-radius:5px}")
        self.scan();self.timer=QTimer(self);self.timer.timeout.connect(self.tick);self.timer.start(150)
    def scan(self):
        self.items=windows();self.combo.clear()
        for h,t in self.items:self.combo.addItem(t,h)
    def tick(self):
        try:
            path=live_file();d=json.loads(path.read_text(encoding="utf-8")) if path else None;h=self.combo.currentData()
            if h and win32gui.IsWindow(h):
                l,t,r,b=win32gui.GetClientRect(h);sx,sy=win32gui.ClientToScreen(h,(0,0))
                if r<=0 or b<=0:return
                with mss.mss() as s:snap=s.grab({"left":sx,"top":sy,"width":r,"height":b})
                im=QImage(snap.rgb,snap.width,snap.height,snap.width*3,QImage.Format_RGB888).copy()
                self.view.update_view(im,d);self.status.setText("ALEX / COLLEGATO" if d else "ALEX / DATI MOD NON TROVATI")
            else:self.view.update_view(None,d);self.status.setText("ALEX / MINECRAFT NON TROVATO")
        except Exception as e:self.status.setText("ALEX / ERRORE: "+str(e)[:90])
if __name__=="__main__":
    q=QApplication(sys.argv);a=App();a.show();sys.exit(q.exec())
