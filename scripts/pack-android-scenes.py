#!/usr/bin/env python3
"""Pack a portable campaign as a tiny manifest plus independently readable scenes."""
import json,sys
from pathlib import Path

def pack(source,destination):
    with Path(source).open(encoding='utf-8') as reader:game=json.load(reader)
    root=Path(destination);(root/'scenes').mkdir(parents=True,exist_ok=True)
    scenes=game.pop('scenes');files={};descriptors=[]
    for index,scene in enumerate(scenes):
        identifier=scene['id']
        if identifier in files:raise ValueError('Duplicate scene id')
        path=f'scenes/{index:04d}.json';files[identifier]=path
        (root/path).write_text(json.dumps(scene,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
        descriptors.append({key:scene[key] for key in ('id','name','worldWidth','worldHeight','background') if key in scene})
    game['scenes']=descriptors;game['sceneFiles']=files
    (root/'game.json').write_text(json.dumps(game,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
    print(f'Android manifest: {(root/"game.json").stat().st_size} bytes; {len(scenes)} independent scenes')

if __name__=='__main__':pack(sys.argv[1],sys.argv[2])
