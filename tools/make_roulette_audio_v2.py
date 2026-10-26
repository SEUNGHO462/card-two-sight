"""Original synthetic mechanical trigger and shot effects, no borrowed audio."""
from pathlib import Path
import math,random,struct,wave

RATE=44100
ROOT=Path(__file__).resolve().parents[1]/'assets/audio'
ROOT.mkdir(parents=True,exist_ok=True)
rng=random.Random(271026)
def save(name,seconds,func):
    values=[func(i/RATE) for i in range(int(RATE*seconds))]
    peak=max(max(abs(v) for v in values),.001)
    with wave.open(str(ROOT/(name+'.wav')),'wb') as f:
        f.setparams((1,2,RATE,len(values),'NONE','not compressed'))
        f.writeframes(b''.join(struct.pack('<h',int(v/peak*29000)) for v in values))

def metal(t):
    ring=sum(math.sin(2*math.pi*h*t)*math.exp(-t*d) for h,d in [(1900,65),(3350,95),(5100,130)])*.18
    impact=rng.uniform(-1,1)*math.exp(-t*360)*.75
    latch=0 if t<.028 else .24*rng.uniform(-1,1)*math.exp(-(t-.028)*220)
    return ring+impact+latch
def shot(t):
    crack=rng.uniform(-1,1)*math.exp(-t*80)*.85
    thump=math.sin(2*math.pi*(100*t-35*t*t))*math.exp(-t*18)*.55
    room=rng.uniform(-1,1)*math.exp(-t*13)*.13
    return crack+thump+room
save('trigger-empty-v2',.22,metal)
save('trigger-shot-v2',.65,shot)
print('Created two original roulette sound effects.')
