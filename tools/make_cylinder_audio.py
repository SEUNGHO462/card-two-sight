"""Original short synthetic loading and locking cues."""
from pathlib import Path
import math,random,struct,wave
root=Path(__file__).resolve().parents[1]/'assets/audio'
root.mkdir(parents=True,exist_ok=True)
rng=random.Random(261026)
for name,freq,duration in [('cylinder-load-live',1800,.11),('cylinder-load-blank',3100,.08),('cylinder-lock',1350,.18)]:
    data=[]
    for i in range(int(44100*duration)):
        t=i/44100
        v=(.45*math.sin(2*math.pi*freq*t)+.20*math.sin(2*math.pi*freq*1.57*t)+.25*rng.uniform(-1,1))*math.exp(-t*55)
        data.append(struct.pack('<h',int(v*25000)))
    with wave.open(str(root/(name+'.wav')),'wb') as f:
        f.setparams((1,2,44100,len(data),'NONE','not compressed'));f.writeframes(b''.join(data))
print('Three original cylinder sounds created.')
