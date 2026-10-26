"""프로젝트 자체 제작 효과음. 이미지나 외부 녹음 파일을 수정하지 않는다."""
import math
import random
import struct
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "assets" / "audio"
ROOT.mkdir(parents=True, exist_ok=True)
RATE = 22050
rng = random.Random(20261026)

def save(name, seconds, sample):
    frames = []
    for i in range(int(seconds * RATE)):
        value = max(-1, min(1, sample(i / RATE)))
        frames.append(struct.pack("<h", int(value * 22000)))
    with wave.open(str(ROOT / (name + ".wav")), "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(RATE)
        f.writeframes(b"".join(frames))

save("click", .08, lambda t: .3 * math.sin(2*math.pi*720*t) * math.exp(-t*65))
save("blank", .16, lambda t: (.45*rng.uniform(-1,1)+.15*math.sin(2*math.pi*180*t))*math.exp(-t*50))
save("shot", .32, lambda t: (.6*rng.uniform(-1,1)+.3*math.sin(2*math.pi*75*t))*math.exp(-t*24))
save("spin", .85, lambda t: .25*rng.uniform(-1,1)*max(0,math.cos(2*math.pi*(18*t-5*t*t)))**12*math.exp(-t*1.1))
notes = [523.25, 659.25, 783.99, 1046.5]
save("win", .65, lambda t: .25*math.sin(2*math.pi*notes[min(3,int(t/.15))]*t)*math.exp(-(t%.15)*8)*min(1,t*50))
print("Created 5 original UI/game sound effects.")
