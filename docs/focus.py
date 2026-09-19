# 레포 중심 그림 — 가운데에 그 레포, 주변에 직접 연결된 것만
from PIL import ImageFont
import cairosvg
FONT="Noto Sans CJK KR, 'Malgun Gothic', 'Apple SD Gothic Neo', sans-serif"
_f=ImageFont.truetype("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",40)
def tw(s,size): return _f.getlength(s)*size/40
C={"ext":("#F3F4F6","#9CA3AF"),"edge":("#DBEAFE","#3B82F6"),"plat":("#E0E7FF","#6366F1"),"dom":("#DCFCE7","#22C55E"),
   "domn":("#F0FDF4","#86EFAC"),"data":("#FFEDD5","#F97316"),"obs":("#F3E8FF","#A855F7"),"fut":("#FFFFFF","#9CA3AF"),
   "lib":("#FEF3C7","#D97706"),"me":("#FFFFFF","#111827")}
colors=["#374151","#3B82F6","#6366F1","#16A34A","#F97316","#A855F7","#9CA3AF","#D97706","#111827"]
DEFS="".join(f'<marker id="ah-{c[1:]}" markerWidth="11" markerHeight="9" refX="10" refY="4.5" orient="auto"><path d="M0,0 L11,4.5 L0,9 z" fill="{c}"/></marker><marker id="as-{c[1:]}" markerWidth="11" markerHeight="9" refX="1" refY="4.5" orient="auto"><path d="M11,0 L0,4.5 L11,9 z" fill="{c}"/></marker>' for c in colors)

class D:
    def __init__(s,W,H): s.W,s.H,s.out,s.N=W,H,[],{}
    def text(s,x,y,t,size=14,w="normal",anchor="middle",fill="#111827",bg=False):
        t=t.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
        if bg:
            ww=tw(t,size)+10; x0=x-ww/2 if anchor=="middle" else (x-4 if anchor=="start" else x-ww+4)
            s.out.append(f'<rect x="{x0:.0f}" y="{y-size+1:.0f}" width="{ww:.0f}" height="{size+6}" rx="4" fill="#FFFFFF" opacity="0.92"/>')
        s.out.append(f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{w}" text-anchor="{anchor}" fill="{fill}">{t}</text>')
    def node(s,name,cx,cy,w,h,kind,title,sub=None,dash=False,tsize=17,sw=2.2):
        f,st=C[kind]; d=' stroke-dasharray="9,7"' if dash else ''
        s.out.append(f'<rect x="{cx-w/2}" y="{cy-h/2}" width="{w}" height="{h}" rx="12" fill="{f}" stroke="{st}" stroke-width="{sw}"{d}/>')
        lines=sub.split("|") if sub else []
        y0=cy-6-8*len(lines)+ (0 if lines else 12)
        s.text(cx,y0,title,tsize,"bold")
        for i,l in enumerate(lines): s.text(cx,y0+20+i*16,l,12,fill="#4B5563")
        s.N[name]=(cx,cy,w,h)
    def me(s,name,cx,cy,w,h,kind,title,sub=None):
        f,st=C[kind]
        s.out.append(f'<rect x="{cx-w/2-8}" y="{cy-h/2-8}" width="{w+16}" height="{h+16}" rx="18" fill="none" stroke="{st}" stroke-width="3" opacity="0.5"/>')
        s.node(name,cx,cy,w,h,kind,title,sub,tsize=22,sw=3.5)
        s.text(cx-w/2-8,cy-h/2-14,"이 레포",13,"bold","start",st)
    def side(s,name,which):
        cx,cy,w,h=s.N[name]
        return {"l":(cx-w/2,cy),"r":(cx+w/2,cy),"t":(cx,cy-h/2),"b":(cx,cy+h/2)}[which]
    def edge(s,a,sa,b,sb,color,label=None,dash=False,both=False,w=2.4,via=None,lx=None,ly=None,anchor="middle",lsize=13,a_pt=None,b_pt=None):
        p1=a_pt or s.side(a,sa); p2=b_pt or s.side(b,sb); pts=[p1]+(via or [])+[p2]
        d=' stroke-dasharray="7,6"' if dash else ''; ms=f' marker-start="url(#as-{color[1:]})"' if both else ''
        s.out.append(f'<polyline points="{" ".join(f"{x},{y}" for x,y in pts)}" fill="none" stroke="{color}" stroke-width="{w}" marker-end="url(#ah-{color[1:]})"{ms}{d}/>')
        if label:
            if lx is None:
                mid=pts[len(pts)//2] if len(pts)>2 else ((p1[0]+p2[0])/2,(p1[1]+p2[1])/2); lx,ly=mid[0],mid[1]-8
            s.text(lx,ly,label,lsize,fill=color,anchor=anchor,bg=True)
    def note(s,x,y,t,color="#4B5563",size=13,anchor="start"): s.text(x,y,t,size,fill=color,anchor=anchor)
    def save(s,name,foot):
        svg=f'''<svg xmlns="http://www.w3.org/2000/svg" width="{s.W}" height="{s.H}" viewBox="0 0 {s.W} {s.H}" font-family="{FONT}">
<defs>{DEFS}</defs><rect width="{s.W}" height="{s.H}" fill="#FFFFFF"/>
{chr(10).join(s.out)}
<text x="{s.W-24}" y="{s.H-12}" font-size="11" text-anchor="end" fill="#9CA3AF">{foot}</text></svg>'''
        open(f"focus-{name}.svg","w",encoding="utf-8").write(svg)
        cairosvg.svg2png(url=f"focus-{name}.svg",write_to=f"focus-{name}.png",output_width=2000)
        print("ok",name)

B,V,G,O,P,X,L="#3B82F6","#6366F1","#16A34A","#F97316","#A855F7","#9CA3AF","#D97706"

# ── gateway-server
d=D(1500,760)
d.me("gw",750,380,340,100,"edge","gateway-server  :8080","JWT 검증 · 라우팅 · 헤더 주입|WebFlux · 상태 없음")
d.node("br",180,180,240,80,"ext","브라우저","쿠키에 JWT")
d.node("ng",180,380,240,80,"fut","nginx","예정",dash=True)
d.node("cf",750,120,300,80,"plat","config-server  :8888","라우트 19 · 공개키 · permit-all 9")
d.node("eu",1300,120,300,80,"plat","eureka-server  :8761","이름 → 주소")
d.node("dom",1300,380,300,110,"dom","도메인 서비스 14개","auth · user · pet · place …|헤더만 믿고 토큰은 안 봄")
d.node("zk",1300,620,300,80,"obs","Zipkin","추적이 여기서 시작됨")
d.node("au",180,620,260,80,"dom","auth-service","공개키의 짝인 개인키로 서명")
d.edge("br","r","gw","l",B,"① 모든 요청  (지금은 8080 직접)",via=[(580,180),(580,340)],lx=430,ly=170)
d.edge("ng","r","gw","l",X,"예정",dash=True)
d.edge("cf","b","gw","t",V,"기동 시 설정")
d.edge("gw","r","eu","l",V,"② \"place 어디 있어?\"",via=[(1000,380),(1000,120)],lx=1000,ly=250)
d.edge("gw","r","dom","l",G,"③ 라우팅  +  X-User-Id · X-User-Role",w=3.5,lx=980,ly=372)
d.edge("gw","b","zk","t",P,"traceId 여기서 생성",dash=True,a_pt=(850,430),via=[(850,530),(1300,530)],lx=1080,ly=522)
d.edge("au","t","gw","b",L,"같은 키 쌍 (auth 개인키 · 여기 공개키)",dash=True,via=[(180,530),(650,530)],b_pt=(650,430),lx=420,ly=522)
d.note(40,720,"바깥에서 들어온 X-User-Id 는 여기서 지워짐 · permit-all 9줄은 auth 와 같은 목록 · 401 · 403 · 404 · 503 을 직접 냄")
d.save("gateway-server","gateway-server 를 중심으로 · 직접 연결된 것만")

# ── auth-service
d=D(1500,820)
d.me("au",750,410,340,100,"dom","auth-service  :8081","가입 · 로그인 · 토큰 발급 · 탈퇴|API 16개 · 서비스 11개")
d.node("gw",180,410,240,90,"edge","gateway-server","공개키로 검증|X-User-Id 헤더 주입")
d.node("cf",750,120,300,80,"plat","config-server","JWT · 메일 · OAuth 설정")
d.node("eu",1300,120,300,80,"plat","eureka-server","등록")
d.node("pg",1300,330,300,80,"data","PostgreSQL  auth_db","account · refresh_token_log · outbox")
d.node("rd",1300,470,300,80,"data","Redis","토큰 · 인증 코드 · state  (9종)")
d.node("kf",1300,640,300,90,"data","Kafka","account.created → user|account.withdrawn → 5개 서비스")
d.node("gg",180,150,240,80,"ext","Google OAuth","소셜 로그인",dash=True)
d.node("gm",180,660,240,80,"ext","Gmail SMTP","인증 코드 메일",dash=True)
d.edge("gw","r","au","l",B,"로그인 · 가입은 토큰 없이 · /me · 탈퇴는 헤더로",lx=430,ly=395)
d.edge("cf","b","au","t",V,"기동 시 설정")
d.edge("au","r","eu","l",V,"등록",via=[(1000,410),(1000,120)],lx=1000,ly=260)
d.edge("au","r","pg","l",O,"JPA · Flyway V20~23",via=[(1000,410),(1000,330)],lx=1010,ly=340)
d.edge("au","r","rd","l",O,"TTL 값",via=[(1000,410),(1000,470)],lx=1010,ly=490)
d.edge("au","r","kf","l",O,"Outbox 로 발행  (받는 것은 없음)",via=[(1000,410),(1000,640)],lx=1010,ly=660)
d.edge("au","l","gg","r",X,"authorize · callback",dash=True,via=[(430,410),(430,150)],lx=440,ly=270)
d.edge("au","l","gm","r",X,"6자리 코드",dash=True,via=[(430,410),(430,660)],lx=440,ly=560)
d.note(40,790,"다른 도메인 서비스를 한 번도 부르지 않음 · 개인키는 환경변수, 공개키는 config 저장소 · 유일하게 자기 SecurityFilterChain 을 정의함")
d.save("auth-service","auth-service 를 중심으로 · 직접 연결된 것만")

# ── config-server
d=D(1500,700)
d.me("cs",750,350,340,100,"plat","config-server  :8888","저장소를 읽어 4계층을 겹쳐 내려 줌|자바 파일 1개")
d.node("gh",180,350,260,90,"ext","GitHub","paw-trail/config|yml 23개 · main")
d.node("all",1300,250,320,110,"dom","다른 서비스 전부","도메인 14 · gateway · eureka|기동할 때 여기로 물어봄")
d.node("eu",1300,520,300,80,"plat","eureka-server","등록만 함 (아무도 안 찾음)")
d.node("me2",750,120,340,80,"fut","이 서버의 application.yml","자기 설정은 저장소에서 못 받음 (닭-달걀)",dash=True)
d.edge("gh","r","cs","l",V,"clone-on-start · 요청마다 다시 읽음")
d.edge("all","l","cs","r",V,"GET /{서비스명}/{환경}",via=[(1000,250),(1000,350)],lx=1000,ly=290,both=False)
d.edge("cs","r","eu","l",V,"등록 · 하트비트",via=[(1000,350),(1000,520)],lx=1000,ly=445)
d.edge("me2","b","cs","t",X,"git.uri · 포트 · EUREKA_HOST · LOKI_HOST",dash=True,lx=760,ly=250)
d.note(40,660,"${환경변수} 는 치환하지 않고 문자열 그대로 내려보냄 → 비밀을 몰라도 됨 → 저장소를 공개로 둘 수 있음")
d.save("config-server","config-server 를 중심으로 · 직접 연결된 것만")

# ── eureka-server
d=D(1500,700)
d.me("eu",750,350,340,100,"plat","eureka-server  :8761","이름 → 주소 장부|자기는 장부에 안 올림")
d.node("gw",180,200,260,90,"edge","gateway-server","\"place 어디 있어?\"|lb://place-service")
d.node("dom",180,500,260,110,"dom","도메인 서비스 14개","기동할 때 등록|30초마다 하트비트")
d.node("cf",750,120,300,80,"plat","config-server","eureka-server.yml + 4계층 my-url")
d.node("cs",1300,500,300,80,"plat","config-server 도 등록","대시보드에 보이기 위해")
d.node("dash",1300,200,300,90,"obs","대시보드  :8761","등록 목록 · DS Replicas 는 비어야 정상")
d.edge("gw","r","eu","l",V,"② 조회",via=[(500,200),(500,350)],lx=500,ly=270)
d.edge("dom","r","eu","l",V,"등록 · 하트비트 · 90초 없으면 만료",via=[(500,500),(500,350)],lx=500,ly=440)
d.edge("cf","b","eu","t",V,"기동 시 설정  (my-url 이 없으면 기동 실패)")
d.edge("cs","l","eu","r",V,"등록만",via=[(1000,500),(1000,350)],lx=1000,ly=440)
d.edge("eu","r","dash","l",P,"",via=[(1000,350),(1000,200)])
d.note(40,660,"자기보호 모드를 껐음 · local 은 host.docker.internal 로, dev 는 컨테이너 IP 로 등록됨 · 피어 복제 없음 (1대)")
d.save("eureka-server","eureka-server 를 중심으로 · 직접 연결된 것만")

# ── common
d=D(1500,760)
d.me("cm",750,380,340,110,"lib","common  0.0.9","자동 설정 6개 · Flyway V1 · V2|실행되지 않는 jar")
d.node("gp",750,120,320,80,"ext","GitHub Packages","publish 로 올림 · 덮어쓰기 불가")
d.node("dom",1300,240,320,110,"dom","도메인 서비스 14개","build.gradle 한 줄로 받음|응답 형식 · 예외 · 인증 · 감사 · Outbox")
d.node("nodb",1300,520,320,90,"domn","무상태 서비스 3개","verdict · congestion · route|JPA 자동 설정만 안 켜짐")
d.node("plat",180,240,280,110,"plat","플랫폼 3개","gateway · eureka · config-server|⛔ 안 씀 — 인프라 성격")
d.node("pg",180,520,280,90,"data","PostgreSQL","outbox · processed_event 표|V1 · V2 가 만듦")
d.edge("cm","t","gp","b",L,"./gradlew publish")
d.edge("gp","r","dom","l",L,"commonVersion=0.0.9",via=[(1000,120),(1000,240)],lx=1000,ly=180)
d.edge("cm","r","dom","l",L,"자동 설정 6개 전부",via=[(1000,380),(1000,240)],lx=1010,ly=320,anchor="start")
d.edge("cm","r","nodb","l",L,"Web · Security · Async 만",via=[(1000,380),(1000,520)],lx=1010,ly=460,anchor="start")
d.edge("plat","r","cm","l",X,"의존하지 않음",dash=True,via=[(500,240),(500,380)],lx=500,ly=310)
d.edge("cm","l","pg","r",O,"Flyway V1 · V2 (jar 안에)",via=[(500,380),(500,520)],lx=500,ly=460)
d.note(40,720,"넣는 기준 = \"도메인 서비스가 전부 쓰는 것\" · 토픽 이름 · 도메인 에러 코드는 넣지 않음 · 서비스가 같은 타입의 Bean 을 정의하면 물러남")
d.save("common","common 을 중심으로 · 직접 연결된 것만")

# ── config (저장소)
d=D(1500,760)
d.me("cf",750,380,340,110,"lib","paw-trail/config","yml 23개 · 4계층|코드 없음 · 공개 저장소")
d.node("cs",180,380,280,90,"plat","config-server","요청마다 여기를 읽음")
d.node("all",180,140,280,100,"dom","서비스 17개","각자 application.yml 은 3줄|나머지는 여기서")
d.node("env",750,120,340,80,"ext","환경변수","${DB_HOST} · ${SERVICE_DB_PASSWORD} · ${AUTH_…}",dash=True)
d.node("gw",1300,200,300,90,"edge","gateway-server.yml","라우트 19 · 공개키 · permit-all 9|293줄")
d.node("au",1300,380,300,90,"dom","auth-service.yml","JWT · 메일 · OAuth · permit-all 9|238줄")
d.node("l3",1300,560,300,100,"plat","application-{env}.yml","local · dev · prod|주소만 갈림 · prod 는 거의 TODO")
d.edge("cf","l","cs","r",V,"git clone · pull")
d.edge("cs","t","all","b",V,"GET /{서비스명}/{환경}")
d.edge("env","b","cf","t",X,"값은 여기 없음 — 자리만",dash=True,lx=760,ly=250)
d.edge("cf","r","gw","l",V,"2계층",via=[(1000,380),(1000,200)],lx=1000,ly=280)
d.edge("cf","r","au","l",V,"2계층",lx=1010,ly=372)
d.edge("cf","r","l3","l",V,"3계층",via=[(1000,380),(1000,560)],lx=1000,ly=480)
d.note(40,720,"1 application.yml < 2 {서비스}.yml < 3 application-{env}.yml < 4 {서비스}-{env}.yml — 숫자가 클수록 이김 · 4계층 실사례는 eureka 둘뿐")
d.save("config","config 저장소를 중심으로 · 직접 연결된 것만")

# ── infra
d=D(1500,800)
d.me("inf",750,400,340,110,"lib","paw-trail/infra","docker-compose.yml · 컨테이너 12개|프로파일 6개")
d.node("ij",180,180,280,100,"ext","IntelliJ 로 띄운 서비스","지금 고치는 것|localhost:5432 · :29092 · :8888")
d.node("db",180,400,280,90,"data","db","postgres — DB 10개 · 계정 10개")
d.node("inf2",180,620,280,90,"data","infra","kafka · redis")
d.node("plat",1300,180,300,100,"plat","platform","config-server · eureka-server|gateway-server")
d.node("app",1300,400,300,90,"dom","app","auth-service  (지금은 하나)")
d.node("obs",1300,620,300,100,"obs","observability · tools","prometheus · loki · zipkin · grafana|kafka-ui :9000")
d.edge("inf","l","db","r",O,"항상")
d.edge("inf","l","inf2","r",O,"항상",via=[(500,400),(500,620)],lx=500,ly=520)
d.edge("inf","r","plat","l",V,"항상",via=[(1000,400),(1000,180)],lx=1000,ly=290)
d.edge("inf","r","app","l",G,"COMPOSE_PROFILES 에 app 을 넣을 때",lx=1010,ly=392)
d.edge("inf","r","obs","l",P,"필요할 때만",via=[(1000,400),(1000,620)],lx=1000,ly=520)
d.edge("ij","b","db","t",X,"localhost 로 붙음  (컨테이너 안에서는 postgres)",dash=True,lx=190,ly=300)
d.note(40,760,"--profile 을 명령에 붙이면 .env 값이 대체됨 (더해지지 않음) · .env 는 커밋 안 됨 · Kafka 는 볼륨이 없어 down 하면 토픽이 사라짐 (재생성 멱등)")
d.save("infra","infra 를 중심으로 · 직접 연결된 것만")

# ── service-template
d=D(1500,820)
d.me("st",750,410,340,110,"dom","새 도메인 서비스","service-template 을 복제해 만듦|4계층 · common · Outbox")
d.node("gw",180,410,260,90,"edge","gateway-server","라우트를 열어야 닿음")
d.node("cf",750,130,300,90,"plat","config 저장소","{서비스명}.yml 을 만들어야 뜸|포트 · DB · outbox 스위치")
d.node("eu",1300,130,300,80,"plat","eureka-server","기동하면 자동 등록")
d.node("cm",180,150,260,90,"lib","common","gradle 한 줄 · 자동 설정 6개")
d.node("pg",1300,330,300,90,"data","PostgreSQL","자기 DB 하나 · Flyway V20~")
d.node("kf",1300,530,300,100,"data","Kafka","발행은 Outbox · 소비는 Inbox|토픽은 infra 스크립트에 먼저")
d.node("oth",1300,720,300,80,"dom","다른 도메인 서비스","/internal 로 서로 부름")
d.node("inf",180,680,260,90,"data","infra compose","완성되면 app 프로파일에 등록")
d.edge("gw","r","st","l",B,"③  X-User-Id · X-User-Role 헤더")
d.edge("cf","b","st","t",V,"기동 시 설정")
d.edge("st","r","eu","l",V,"등록",via=[(1000,410),(1000,130)],lx=1000,ly=270)
d.edge("cm","r","st","l",L,"의존성",via=[(500,150),(500,410)],lx=500,ly=280)
d.edge("st","r","pg","l",O,"JPA",via=[(1000,410),(1000,330)],lx=1010,ly=340)
d.edge("st","r","kf","l",O,"이벤트",via=[(1000,410),(1000,530)],lx=1010,ly=480,both=True)
d.edge("st","r","oth","l",G,"/internal · 헤더 그대로 전달",via=[(1000,410),(1000,720)],lx=1010,ly=640,both=True)
d.edge("st","b","inf","t",X,"이미지로 구운 뒤",dash=True,via=[(750,600),(180,600)],lx=470,ly=592)
d.note(40,790,"복제 후 할 일 8개 (settings · gradle.properties · yml · Dockerfile · Jenkinsfile · V20 · README) · DB 없는 서비스는 데이터 블록을 통째로 지움")
d.save("service-template","새 서비스를 중심으로 · 직접 연결된 것만")

# ── user-service
d=D(1640,900)
d.me("us",800,440,360,100,"dom","user-service  :8082","프로필 · 즐겨찾기 · 방문 · 일정 · 요약|API 20개 · 서비스 7개")
d.node("gw",180,440,240,90,"edge","gateway-server","토큰 검증|X-User-Id 헤더 주입")
d.node("cf",800,120,300,80,"plat","config-server","포트 · DB · S3 · LLM 설정")
d.node("eu",1400,120,300,80,"plat","eureka-server","등록 · lb:// 해석")
d.node("pg",1400,300,300,90,"data","PostgreSQL  user_db","프로필 · 즐겨찾기 · 방문|일정 · 요약  (표 5개)")
d.node("rd",1400,470,300,80,"data","Redis","최근 장소 · 요약 쿨다운 · 한도")
d.node("s3",180,150,240,80,"ext","AWS S3","프로필 사진",dash=True)
d.node("ai",180,700,240,80,"ext","OpenAI","하루 요약 문장",dash=True)
d.node("kf",800,760,300,90,"data","Kafka","account.created  수신|account.withdrawn  수신")
d.node("pl",1400,690,300,110,"domn","place · verdict · review","이름 · 사진 · 좌표|판정 · 준비물 · 평점|review 만 아직 없음 — 스텁으로 흉내",dash=True)
d.edge("gw","r","us","l",B,"공개 API 18개 · 무인증 경로 0",lx=470,ly=422)
d.edge("cf","b","us","t",V,"기동 시 설정")
d.edge("us","r","eu","l",V,"등록",via=[(1080,440),(1080,120)],lx=1160,ly=205)
d.edge("us","r","pg","l",O,"JPA · Flyway V20~21",via=[(1080,440),(1080,300)],lx=1160,ly=378)
d.edge("us","r","rd","l",O,"키 3종",via=[(1080,440),(1080,470)],lx=1160,ly=462)
d.edge("us","r","pl","l",G,"lb:// 로 부름",via=[(1080,440),(1080,690)],lx=1160,ly=596)
d.edge("us","b","kf","t",O,"Inbox 로 수신  (발행하지 않음)",lx=812,ly=640,anchor="start")
d.edge("us","l","s3","r",X,"presigned URL",dash=True,via=[(470,440),(470,150)],lx=480,ly=280,anchor="start")
d.edge("us","l","ai","r",X,"gpt-5.6-luna · 20초",dash=True,via=[(470,440),(470,700)],lx=480,ly=600,anchor="start")
d.note(40,858,"프로필을 만드는 API 가 없음 — account.created 로 생기고 account.withdrawn 으로 지워짐 · 소프트 딜리트는 user_profile 하나뿐 · 이벤트를 발행하지 않아 outbox 는 비어 있음")
d.note(40,880,"목록 조회 하나가 place · verdict · review 셋을 부름 — place 가 실패하면 목록 전체가 실패하고 나머지 둘은 그 값만 비움")
d.save("user-service","user-service 를 중심으로 · 직접 연결된 것만")

# ── place-service
d=D(1860,960)
d.me("pc",820,450,360,110,"dom","place-service  :8084",
     "여러 소스가 가리키는 같은 곳을 하나로|API 12개 · 서비스 8개")
d.node("gw",180,450,240,90,"edge","gateway-server","토큰 검증|X-User-Id · X-User-Role 주입")
d.node("cf",820,120,300,80,"plat","config-server","포트 · DB · 카카오 키")
d.node("eu",1560,120,300,80,"plat","eureka-server","등록 · lb:// 해석")
d.node("pg",1560,300,300,110,"data","PostgreSQL  place_db",
       "place 19,501행 · 소스 연결 · 편의시설|반영 대기 · 분리 이력  (표 5개 + outbox)|좌표 출처 셋 — 원본 · 변환 · 지오코딩")
d.node("ka",180,150,240,80,"ext","카카오 로컬","좌표가 없는 장소를 주소로",dash=True)
d.node("ig",1560,620,300,110,"domn","ingest-service",
       "적재를 넘겨 받음 (bulk)|원문을 내어 줌 (documents)|평소에는 안 떠 있음")
d.node("us",180,700,240,100,"domn","user-service","즐겨찾기 · 방문 · 일정이|장소 이름과 사진을 물어봄")
d.node("kf",700,830,300,80,"data","Kafka","place.updated 발행|받는 것은 없음")
d.node("se",1300,830,300,80,"domn","search-service","place.updated 를 받으면 다시 읽어 색인|GET /internal/places/indexing")
d.edge("gw","r","pc","l",B,"공개 2 · 관리자 7",lx=480,ly=432)
d.edge("cf","b","pc","t",V,"기동 시 설정")
d.edge("pc","r","eu","l",V,"등록",via=[(1120,450),(1120,120)],lx=1200,ly=205)
d.edge("pc","r","pg","l",O,"JPA · PostGIS · Flyway V20~27",
       via=[(1120,450),(1120,300)],lx=1200,ly=378)
d.edge("ig","l","pc","r",G,"POST /internal/places/bulk   담아 둔 것과 바로 보낸 것 둘 다",
       a_pt=(1410,590),via=[(1260,590),(1260,470)],b_pt=(1000,470),lx=1270,ly=545,anchor="start")
d.edge("pc","r","ig","l",G,"GET /internal/raw/{placeId}/documents",
       a_pt=(1000,505),via=[(1160,505),(1160,650)],b_pt=(1410,650),lx=1170,ly=700,anchor="start")
d.edge("us","r","pc","l",G,"GET /internal/places?ids=",
       via=[(480,700),(480,480)],b_pt=(640,480),lx=310,ly=660,anchor="start")
d.edge("pc","b","kf","t",O,"Outbox 로 발행  (수신하지 않음)",
       a_pt=(900,505),via=[(900,660),(780,660)],b_pt=(780,790),lx=910,ly=600,anchor="start")
d.edge("kf","r","se","l",O,"place.updated",lx=1000,ly=822,anchor="start")
d.edge("pc","l","ka","r",X,"지오코딩 · 3초",dash=True,
       via=[(480,450),(480,150)],lx=490,ly=300,anchor="start")
d.note(40,880,"소스가 넷이라 같은 곳이 네 번 들어옴 — 주소가 같고 이름이 같으면 합치고, 아니면 100m 안에서 이름이 같을 때만 합침 · 관리자가 고친 장소는 잠겨 수집이 건드리지 않고 대기 목록에 쌓임")
d.note(40,902,"좌표 출처가 셋이고 조합마다 기준이 다름 — 원본끼리와 변환이 낀 것은 100m, 지오코딩이 낀 것은 300m, 지오코딩끼리는 합치지 않음")
d.note(40,924,"이 서비스만 Outbox Relay 가 켜져 있음 — 인스턴스를 늘리면 같은 행을 두 번 집으므로 한 대만 켜지도록 갈라야 함")
d.save("place-service","place-service 를 중심으로 · 직접 연결된 것만")

# ── pet-service
d=D(1860,960)
d.me("pt",820,450,360,110,"dom","pet-service  :8083",
     "판정 입력값의 소유자 — 체중 · 장비 · 접종|API 11개  (공개 7 · internal 2 · 관리자 2)")
d.node("gw",180,450,240,90,"edge","gateway-server","토큰 검증|X-User-Id · X-User-Role 주입")
d.node("cf",820,120,300,80,"plat","config-server","포트 · DB · S3 키")
d.node("eu",1560,120,300,80,"plat","eureka-server","등록 · lb:// 해석")
d.node("pg",1560,300,300,110,"data","PostgreSQL  pet_db",
       "pet · breed  (표 2개 + outbox · inbox)|견종 마스터 45행 — 맹견 5종 포함|크기는 체중에서 계산해 채움")
d.node("s3",180,150,240,80,"ext","AWS S3","반려동물 사진|브라우저가 직접 올림")
d.node("rv",1560,620,300,110,"fut","review-service",
       "후기를 쓸 때 견종 · 체중을|스냅샷으로 복사해 감|아직 없음",dash=True)
d.node("vd",180,700,240,100,"domn","verdict-service","판정 재료로|반려동물을 한 번에 물어봄|100마리까지")
d.node("kf",700,810,300,80,"data","Kafka","pet.profile.updated 발행|account.withdrawn 수신")
d.node("au",1300,810,300,80,"domn","auth-service","탈퇴를 알림")
d.edge("gw","r","pt","l",B,"공개 7 · 관리자 2",lx=480,ly=434)
d.edge("cf","b","pt","t",V,"기동 시 설정")
d.edge("pt","r","eu","l",V,"등록",via=[(1120,450),(1120,120)],lx=1200,ly=205)
d.edge("pt","r","pg","l",O,"JPA · Flyway V20~21",via=[(1120,450),(1120,300)],lx=1200,ly=378)
d.edge("rv","l","pt","r",G,"GET /internal/pets?ids=   상한 100 · 소유권 검증",dash=True,
       a_pt=(1410,590),via=[(1260,590),(1260,470)],b_pt=(1000,470),lx=1272,ly=545,anchor="start")
d.edge("vd","r","pt","l",G,"GET /internal/pets?ids=",
       via=[(480,700),(480,480)],b_pt=(640,480),lx=494,ly=592,anchor="start")
d.edge("pt","b","kf","t",O,"Outbox 로 발행",
       a_pt=(900,505),via=[(900,660),(790,660)],b_pt=(790,770),lx=912,ly=600,anchor="start")
d.edge("kf","t","pt","b",O,"account.withdrawn 을 받아 지움",
       a_pt=(610,770),via=[(610,620),(700,620)],b_pt=(700,505),lx=598,ly=632,anchor="end")
d.edge("au","l","kf","r",X,"account.withdrawn",lx=1010,ly=802,anchor="start")
d.edge("pt","l","s3","r",X,"주소만 발급 · 탈퇴 시 객체 삭제",dash=True,
       a_pt=(640,398),via=[(430,398),(430,150)],lx=492,ly=300,anchor="start")
d.note(40,880,"크기는 견종이 아니라 체중으로만 가름 — SMALL 10kg 미만 · MEDIUM 10~25kg 미만 · LARGE 25kg 이상 · 서버가 채우되 사용자가 고친 값이 이김")
d.note(40,902,"견종 마스터는 맹견 판정과 종 구분에만 쓰임 — 크기 기본값 컬럼을 두지 않아 견종을 늘리거나 줄여도 판정이 달라지지 않음")
d.note(40,924,"내어 주는 두 API 는 소유권 검증이 필수임 — 없으면 아무 식별자나 넣어 남의 반려동물 기준으로 판정을 받아볼 수 있음")
d.save("pet-service","pet-service 를 중심으로 · 직접 연결된 것만")

# ── policy-service
d=D(1860,960)
d.me("po",820,450,360,110,"dom","policy-service  :8085",
     "장소마다 동반 조건을 한 벌로 합쳐 둠|API 8개  (공개 1 · internal 2 · 관리자 5)")
d.node("gw",180,450,240,90,"edge","gateway-server","토큰 검증|X-User-Id · X-User-Role 주입")
d.node("cf",820,120,300,80,"plat","config-server","포트 · DB")
d.node("eu",1560,120,300,80,"plat","eureka-server","등록 · lb:// 해석")
d.node("pg",1560,300,300,110,"data","PostgreSQL  policy_db",
       "소스별 조건 · 합친 조건 · 근거 · 충돌 · 정정 이력|표 5개 + outbox · inbox|조건 20칸 — null 은 정보 없음")
d.node("ex",1560,620,300,110,"domn","extract-service",
       "원문에서 동반 조건을 뽑아|소스마다 한 벌씩 넣음|규칙 + 모델 두 번 읽기")
d.node("vd",180,700,240,100,"domn","verdict-service","판정할 때 조건 20칸과|근거를 한 번에 물어봄|500곳까지")
d.node("kf",700,810,300,80,"data","Kafka","policy.changed 발행|받는 것은 없음")
d.node("nt",1300,810,300,80,"fut","notification-service","즐겨찾기한 사람에게 알림|아직 없음",dash=True)
d.edge("gw","r","po","l",B,"공개 1 · 관리자 5",lx=480,ly=434)
d.edge("cf","b","po","t",V,"기동 시 설정")
d.edge("po","r","eu","l",V,"등록",via=[(1120,450),(1120,120)],lx=1200,ly=205)
d.edge("po","r","pg","l",O,"JPA · Flyway V20~25 · 장소 잠금",via=[(1120,450),(1120,300)],lx=1200,ly=378)
d.edge("ex","l","po","r",G,"POST /internal/policies/bulk   청크 100 · 상한 500",
       a_pt=(1410,590),via=[(1260,590),(1260,470)],b_pt=(1000,470),lx=1272,ly=545,anchor="start")
d.edge("vd","r","po","l",G,"POST /internal/policies/batch",
       via=[(480,700),(480,480)],b_pt=(640,480),lx=494,ly=592,anchor="start")
d.edge("po","b","kf","t",O,"Outbox 로 발행  (수신하지 않음)",
       a_pt=(900,505),via=[(900,660),(790,660)],b_pt=(790,770),lx=912,ly=600,anchor="start")
d.edge("kf","r","nt","l",X,"policy.changed",dash=True,lx=1010,ly=802,anchor="start")
d.edge("kf","l","vd","b",X,"policy.changed  캐시를 붙일 때 받음",dash=True,
       via=[(180,810)],lx=370,ly=802,anchor="middle")
d.note(40,880,"조건은 소스마다 한 벌씩 받아 한 벌로 합침 — OWNER > MANUAL > 공공 3종 · 공공끼리는 칸마다 채우고, 값이 갈리면 충돌로 남겨 배지를 붙임")
d.note(40,902,"null 과 false 는 다른 값임 — null 은 정보 없음, false 는 요구하지 않음 · 관리자 정정은 20칸 전체 교체라 비워 둔 칸도 뜻을 가짐")
d.note(40,924,"batch 가 내보내는 것(조건 · 충돌 여부 · 최상위 티어 · 근거)이 바뀔 때만 판을 올리고 policy.changed 를 냄 · 한 장소의 쓰기는 장소 잠금으로 한 줄로 섬")
d.save("policy-service","policy-service 를 중심으로 · 직접 연결된 것만")


# ── ingest-service
d=D(1980,1010)
d.me("ig",840,470,380,110,"dom","ingest-service  :8088",
     "공공데이터를 받아 담고 place 로 넘김|API 5개 · 화면 없음 · 상시 미기동")

d.node("jk",180,140,260,90,"fut","Jenkins 잡","수집을 언제 부를지 정함|아직 없음",dash=True)
d.node("api",180,360,260,100,"ext","공공데이터포털","반려동물 동반여행 · 고캠핑|오퍼레이션마다 하루 1,000회",dash=True)
d.node("csv",180,590,260,100,"ext","CSV 파일 둘","문화정보원 · 행정안전부|이미지 안에 담겨 있음")
d.node("gw",180,830,260,70,"edge","gateway-server","여기로는 라우팅하지 않음",dash=True)

d.node("cf",840,120,320,80,"plat","config-server","포트 · DB · 인증키 · 소스별 값")
d.node("eu",1640,110,300,80,"plat","eureka-server","등록")
d.node("pg",1640,310,300,110,"data","PostgreSQL  raw_db",
       "raw_document 17,471건 · place_id 17,463|ingest_run  (표 2개)|소스 넷 가운데 셋만 담김")
d.node("ex",1640,570,300,100,"domn","extract-service","대기 원문을 가져가 조건을 읽음|처리 결과를 상태로 되돌려 씀")
d.node("pl",1640,830,300,110,"domn","place-service","소스가 겹친 것을 한 장소로|넘긴 결과를 돌려줌|동물병원은 바로 받음")

d.edge("jk","r","ig","l",X,"POST /internal/ingest/trigger",dash=True,
       via=[(520,140),(520,440)],b_pt=(650,440),lx=340,ly=132,anchor="start")
d.edge("ig","l","api","r",O,"목록 · 상세를 부름",dash=True,
       a_pt=(650,470),via=[(520,470),(520,360)],lx=340,ly=345,anchor="start")
d.edge("csv","r","ig","l",O,"파일을 읽음",
       via=[(520,590),(520,500)],b_pt=(650,500),lx=530,ly=560,anchor="start")
d.edge("gw","r","ig","b",X,"라우팅하지 않음",dash=True,
       via=[(600,830),(600,560)],b_pt=(760,525),lx=610,ly=730,anchor="start")

d.edge("cf","b","ig","t",V,"기동 시 설정")
d.edge("ig","r","eu","l",V,"등록",via=[(1300,470),(1300,110)],lx=1310,ly=260,anchor="start")
d.edge("ig","r","pg","l",O,"JPA · Flyway V20 · V21",
       via=[(1300,470),(1300,310)],lx=1310,ly=385,anchor="start")
d.edge("ex","l","ig","r",G,"GET /internal/raw · PATCH 로 결과 반영",
       a_pt=(1490,570),via=[(1180,570),(1180,490)],b_pt=(1030,490),lx=1480,ly=562,anchor="end")

# place 로 가는 길이 둘 — 담아 둔 것을 넘기는 것과 바로 보내는 것
d.edge("ig","r","pl","l",G,"LINK  소스 셋을 담아 둔 뒤 넘김",
       a_pt=(1030,512),via=[(1390,512),(1390,800)],b_pt=(1490,800),lx=1400,ly=690,anchor="start")
d.edge("pl","l","ig","r",G,"GET /internal/raw/{placeId}/documents",
       a_pt=(1490,858),via=[(1250,858),(1250,532)],b_pt=(1030,532),lx=1050,ly=850,anchor="start")
d.edge("ig","b","pl","b",G,"DIRECT  raw 를 거치지 않고 바로",
       a_pt=(900,525),via=[(900,940),(1560,940)],b_pt=(1560,885),lx=910,ly=930,anchor="start")

d.note(40,968,"소스가 넷인데 셋만 raw_db 에 담김 — 행정안전부 동물병원은 조건 문장이 없어 해석할 것도 원문으로 보여줄 것도 없어, 읽는 자리에서 바로 place 로 보냄")
d.note(40,990,"관광공사 상세는 한 번에 3,237회라 하루 한도를 넘김 — 그날 받은 데까지 기록하고 다음 날 그 자리부터 이어받음 · 게이트웨이가 /internal 을 라우팅하지 않고 Kafka 도 쓰지 않음")
d.save("ingest-service","ingest-service 를 중심으로 · 직접 연결된 것만")


# ── extract-service
d=D(1860,960)
d.me("ex",820,450,380,110,"dom","extract-service  :8089",
     "원문에서 반려동물 동반 조건을 읽어 policy 에 넣음|API 1개  (internal 트리거) · DB 없음")
d.node("op",180,330,250,90,"ext","운영자","전량 · 다시 뽑기를 부름|limit 로 몇 건만도 됨")
d.node("jk",180,600,250,90,"fut","Jenkins 잡","수집 뒤에 이어 부름|아직 없음",dash=True)
d.node("cf",820,120,320,80,"plat","config-server","포트 · 청크 · 모델 설정")
d.node("eu",1560,120,300,80,"plat","eureka-server","등록 · lb:// 해석")
d.node("oa",1560,320,300,100,"ext","OpenAI  gpt-5.6-luna","같은 조각을 두 번 읽음|추론 medium · high")
d.node("ol",1560,500,300,80,"ext","Ollama  qwen3.8:27b","로컬 모델 · 비교 · 시험용",dash=True)
d.node("po",1560,700,300,110,"domn","policy-service","소스마다 한 벌씩 받아|장소마다 한 벌로 합쳐 둠|조건 20칸 · 근거 · 충돌")
d.node("ig",820,780,340,110,"domn","ingest-service","원문 raw_db|장소에 이어진 대기 원문을 줌|처리 결과를 상태로 받음")
d.edge("op","r","ex","l",B,"POST /internal/extract/trigger  → 202",
       a_pt=(305,330),via=[(470,330),(470,430)],b_pt=(630,430),lx=318,ly=318,anchor="start")
d.edge("jk","r","ex","l",X,"같은 트리거",dash=True,
       a_pt=(305,600),via=[(500,600),(500,470)],b_pt=(630,470),lx=514,ly=560,anchor="start")
d.edge("cf","b","ex","t",V,"기동 시 설정")
d.edge("ex","r","eu","l",V,"등록",via=[(1130,430),(1130,120)],a_pt=(1010,430),lx=1142,ly=205,anchor="start")
d.edge("ex","r","oa","l",P,"POST /v1/chat/completions  두 번",via=[(1170,450),(1170,320)],a_pt=(1010,450),lx=1182,ly=388,anchor="start")
d.edge("ex","r","ol","l",X,"ollama 일 때",dash=True,via=[(1210,470),(1210,500)],a_pt=(1010,470),lx=1262,ly=490,anchor="start")
d.edge("ex","r","po","l",G,"POST /internal/policies/bulk   청크 100",via=[(1250,490),(1250,700)],a_pt=(1010,490),lx=1262,ly=640,anchor="start")
d.edge("ex","b","ig","t",G,"GET /internal/raw · PATCH /internal/raw/status",lx=832,ly=640,anchor="start")
d.note(40,880,"정형 칸은 규칙이, 문장은 모델이 읽음 — 모델 값에는 늘 원문 조각 번호로 근거가 붙고, 번호가 원문에 없으면 값을 버림")
d.note(40,902,"같은 조각을 추론 medium · high 로 두 번 읽어 칸마다 좁은 쪽을 남김 · 정형 칸과 본문이 갈리면 그 칸을 비우고 소스 내 충돌로 보냄")
d.note(40,924,"DB 없음 — 어디까지 했는지는 ingest 원문 상태가 맡음 · 트리거는 바로 202 · 결과는 실행 끝의 요약 로그 한 줄")
d.save("extract-service","extract-service 를 중심으로 · 직접 연결된 것만")


# ── verdict-service
d=D(1860,960)
d.me("vd",820,450,380,110,"dom","verdict-service  :8086",
     "장소마다 · 반려동물마다 동반 가능 여부를 판정|API 2개  (공개 1 · internal 1) · DB · 캐시 없음")
d.node("gw",180,300,250,90,"edge","gateway-server","토큰 검증|X-User-Id · X-User-Role 주입")
d.node("us",180,540,250,100,"domn","user-service","즐겨찾기 · 최근 · 일정 · 방문 카드|판정 배지와 준비물")
d.node("sr",180,760,250,80,"domn","search-service","검색 카드 · 탐색 카운트|판정 필터면 후보 전부")
d.node("cf",820,120,320,80,"plat","config-server","포트 한 줄")
d.node("eu",1560,120,300,80,"plat","eureka-server","등록 · lb:// 해석")
d.node("pt",1560,380,300,110,"domn","pet-service","반려동물을 한 번에 100마리|체중 · 크기 · 맹견 · 이동장 · 접종|남의 것은 조용히 뺌")
d.node("po",1560,640,300,110,"domn","policy-service","장소마다 조건 20칸 · 근거|충돌 여부 · 정정 출처|한 번에 500곳")
d.node("kf",820,780,320,80,"fut","Kafka","policy.changed · pet.profile.updated|캐시를 붙일 때 받음",dash=True)
d.edge("gw","r","vd","l",B,"GET /api/v1/places/{placeId}/verdict",
       via=[(470,300),(470,430)],b_pt=(630,430),lx=318,ly=288,anchor="start")
d.edge("us","r","vd","l",G,"POST /internal/verdicts/batch",
       via=[(560,540),(560,470)],b_pt=(630,470),lx=318,ly=528,anchor="start")
d.edge("sr","r","vd","l",G,"POST /internal/verdicts/batch  500곳씩",
       via=[(600,760),(600,490)],b_pt=(630,490),lx=318,ly=748,anchor="start")
d.edge("cf","b","vd","t",V,"기동 시 설정")
d.edge("vd","r","eu","l",V,"등록",via=[(1130,430),(1130,120)],a_pt=(1010,430),lx=1142,ly=205,anchor="start")
d.edge("vd","r","pt","l",G,"① GET /internal/pets?ids=",
       a_pt=(1010,450),via=[(1200,450),(1200,380)],b_pt=(1410,380),lx=1212,ly=368,anchor="start")
d.edge("vd","r","po","l",G,"② POST /internal/policies/batch",
       a_pt=(1010,480),via=[(1240,480),(1240,640)],b_pt=(1410,640),lx=1252,ly=560,anchor="start")
d.edge("kf","t","vd","b",X,"아직 받지 않음",dash=True,lx=832,ly=640,anchor="start")
d.note(40,880,"반려견마다 네 단계로 판정 — 불가 > 확인 필요 > 조건부 > 가능 · 판정에 필요한 칸이 비면 가능 대신 확인 필요 · 이유 줄마다 근거와 추출 방식")
d.note(40,902,"pet → policy 를 차례로 한 번씩 부름 · 사용자 헤더 둘이 pet 호출에 그대로 따라감 · 헤더가 없으면 401 · 둘 중 하나라도 못 부르면 502")
d.note(40,924,"DB · 캐시 · 이벤트 없음 — 부를 때마다 새로 판정 · 캐시와 이벤트 소비는 부하를 잰 뒤 · 장소 정보는 화면이 가져 place 를 부르지 않음")
d.save("verdict-service","verdict-service 를 중심으로 · 직접 연결된 것만")


# ── search-service
d=D(1860,960)
d.me("se",820,450,380,110,"dom","search-service  :8087",
     "place 를 옮겨 담은 색인으로 찾음|API 7개  (공개 6 · 관리자 1) · 판정은 verdict 에 물음")
d.node("gw",180,300,250,90,"edge","gateway-server","토큰 검증|X-User-Id · X-User-Role 주입")
d.node("kf",180,540,250,90,"data","Kafka","place.updated 를 받음|묶어서 500건씩 · earliest")
d.node("vd",180,770,250,90,"domn","verdict-service","카드마다 반려동물별 판정|충돌 · 준비물 · 한 줄 근거")
d.node("cf",820,120,320,80,"plat","config-server","재색인 시각 · 잠금 만료")
d.node("eu",1560,120,300,80,"plat","eureka-server","등록 · lb:// 해석")
d.node("pg",1560,290,300,100,"data","PostgreSQL  search_db","search_index 한 표 · 24칸|이름 트라이그램 · 좌표 · 소개문 인덱스")
d.node("rd",1560,460,300,90,"data","Redis","조회수 — 전국 · 시도 열쇠|재색인 잠금  (만료 30분)")
d.node("pl",1560,630,300,90,"domn","place-service","색인용 조회|ids 100 · 이어받기 500")
d.node("rv",1560,800,300,90,"fut","review-service","평점 · 후기 수를 100곳씩|아직 없음 — 그동안 평점은 비어 있음",dash=True)
d.edge("gw","r","se","l",B,"검색 · 카운트 · 자동완성 · 인기 · 지역 · 관리자 재색인",
       via=[(470,300),(470,420)],b_pt=(630,420),lx=318,ly=288,anchor="start")
d.edge("kf","r","se","l",O,"place.updated",
       via=[(520,540),(520,460)],b_pt=(630,460),lx=318,ly=528,anchor="start")
d.edge("se","l","vd","r",G,"POST /internal/verdicts/batch  500곳씩",
       a_pt=(630,490),via=[(580,490),(580,770)],lx=318,ly=758,anchor="start")
d.edge("cf","b","se","t",V,"기동 시 설정")
d.edge("se","r","eu","l",V,"등록",via=[(1110,410),(1110,120)],a_pt=(1010,410),lx=1122,ly=205,anchor="start")
d.edge("se","r","pg","l",O,"JDBC · Flyway V20",via=[(1150,435),(1150,290)],a_pt=(1010,435),lx=1162,ly=355,anchor="start")
d.edge("se","r","rd","l",O,"ZINCRBY · SET NX",a_pt=(1010,460),b_pt=(1410,460),lx=1250,ly=450)
d.edge("se","r","pl","l",G,"GET /internal/places/indexing",via=[(1230,485),(1230,630)],a_pt=(1010,485),lx=1242,ly=565,anchor="start")
d.edge("se","r","rv","l",X,"GET /internal/reviews/stats",dash=True,via=[(1190,500),(1190,800)],a_pt=(1010,500),lx=1242,ly=745,anchor="start")
d.note(40,880,"색인에는 동반 조건이 없음 — 카드마다 verdict 에 물어 붙임 · 판정 필터 · 탐색 카운트는 후보 전부를 500곳씩 판정 (전국 3.3초 · 서울 0.56초)")
d.note(40,902,"place.updated 를 받거나 매일 04:00 재색인 때 place 에서 다시 읽어 덮어씀 — place 수정 시각이 더 새 것만 쓰므로 같은 이벤트가 두 번 와도 결과가 같음")
d.note(40,924,"이름 차례는 ko-x-icu — DB 기본 정렬이 한글을 글자 수로 세움 · 조회수는 Redis 에만 · 두 대로 떠도 재색인은 잠금으로 한 번만 돎")
d.save("search-service","search-service 를 중심으로 · 직접 연결된 것만")
