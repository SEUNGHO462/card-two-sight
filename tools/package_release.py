"""Run after gradlew test :lwjgl3:installDist :server:installDist and --smoke.
Does not modify source assets or synced references. Requires Python 3.10+.
"""
from pathlib import Path
import json
import shutil
import zipfile
import hashlib
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT.parent / "output"
DIST = ROOT / "lwjgl3/build/install/CARD_TWO_SIGHT"
PREVIEW = ROOT / ".runtime/final-preview"

def zip_tree(folder, target, prefix, exclude=()):
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for item in sorted(folder.rglob("*")):
            relative=item.relative_to(folder)
            if item.is_file() and not item.is_symlink() and not any(p in exclude for p in relative.parts):
                archive.write(item, str(Path(prefix) / relative))

def write_batch(path, text):
    path.write_bytes(text.replace("\n", "\r\n").encode("ascii"))

def main():
    assert (PREVIEW / "PASS.txt").is_file(), "Complete a fresh --smoke run first"
    assert not (PREVIEW / "FAILED.txt").exists(), "Smoke test failed"
    reports=[]
    for module in ("common", "core", "server"):
        for path in (ROOT / module / "build/test-results/test").glob("TEST-*.xml"):
            test=ET.parse(path).getroot()
            assert test.get("failures")=="0" and test.get("errors")=="0", path
            reports.append({"suite":test.get("name"),"tests":int(test.get("tests"))})
    assert sum(r["tests"] for r in reports)==12
    for source in sorted(PREVIEW.glob("*.png")):
        target=ROOT / "docs/screenshots" / source.name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source,target)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    runtime=OUTPUT / "CARD_TWO_SIGHT_0.1.0"
    runtime.mkdir(exist_ok=True)
    shutil.copytree(DIST, runtime, dirs_exist_ok=True)
    shutil.copytree(ROOT / "docs", runtime / "docs", dirs_exist_ok=True)
    shutil.copy2(ROOT / "assets/fonts/OFL.txt", runtime / "FONT_LICENSE.txt")
    (runtime / "먼저읽기.txt").write_text(
        "CARD TWO SIGHT · 카투사\n\nJava 17 이상이 필요합니다. 압축을 모두 풀고 게임실행.bat을 실행하세요.\n"
        "혼자 확인: 테이블에 앉기 → 캐릭터 선택 → 연습 테이블 입장 → 준비하기 → 게임 시작.\n\n"
        "두 사람 접속: 방장은 주소 127.0.0.1, 내장 서버 켜짐으로 방을 만듭니다.\n"
        "참가자는 방장 PC의 IP 주소와 같은 포트(기본 54555)·비밀번호로 입장합니다.\n"
        "같은 PC에서 두 번 실행해도 됩니다. 같은 내부 네트워크 접속부터 확인하세요.\n"
        "별도 서버실행.bat 사용 시 방 만들기에서 내장 서버를 끄세요.\n\n"
        "F11 전체 화면 / ESC 뒤로 또는 나가기.\n"
        "기록: 사용자 폴더 .card-two-sight/기록.json\n"
        "기본 규칙과 팀 회의가 필요한 설정은 docs/RULES.md에 있습니다.\n"
        "이 실행본은 Windows x64용이며 Java 실행 환경은 별도입니다.\n", encoding="utf-8")
    write_batch(runtime / "게임실행.bat", '@echo off\nsetlocal\ncd /d "%~dp0"\ncall "bin\\CARD_TWO_SIGHT.bat" %*\nif errorlevel 1 (\necho Java 17+ is required. See the error above.\npause\nexit /b 1\n)\n')
    write_batch(runtime / "서버실행.bat", '@echo off\nsetlocal\nchcp 65001 >nul\ncd /d "%~dp0"\nset "C2S_JAVA=java"\nif defined JAVA_HOME set "C2S_JAVA=%JAVA_HOME%\\bin\\java.exe"\n"%C2S_JAVA%" -Dfile.encoding=UTF-8 -cp "lib\\*" kr.ac.c2s.server.ServerLauncher %*\nif errorlevel 1 pause\n')
    validation={"tests":reports,"smoke":(PREVIEW/"PASS.txt").read_text(encoding="utf-8"),"java":"21.0.12","target":"Windows x64, Java 17+"}
    (runtime / "검증결과.json").write_text(json.dumps(validation,ensure_ascii=False,indent=2),encoding="utf-8")
    artifacts=[OUTPUT / "CARD_TWO_SIGHT_실행본.zip",OUTPUT / "CARD_TWO_SIGHT_Java프로젝트.zip"]
    zip_tree(runtime,artifacts[0],runtime.name)
    zip_tree(ROOT,artifacts[1],ROOT.name,{".tools",".gradle",".runtime","build",".git","__pycache__"})
    checksums=[]
    for artifact in artifacts:
        with zipfile.ZipFile(artifact) as archive:
            assert archive.testzip() is None
        checksums.append(hashlib.sha256(artifact.read_bytes()).hexdigest()+"  "+artifact.name)
        print(f"{artifact.name}: {artifact.stat().st_size / 1024**2:.1f} MB")
    (OUTPUT / "CARD_TWO_SIGHT_SHA256.txt").write_text("\n".join(checksums)+"\n",encoding="utf-8")
    print("12 tests passed; source and runtime archives verified.")

if __name__ == "__main__":
    main()
