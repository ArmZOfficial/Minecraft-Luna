"""Rebuild one NPC v2 end to end: fresh project, build, native pose capture, manifest, offline gate.

Only for regenerating our own exported revision (e.g. after a texture pipeline change); it deletes
that revision's generated files first, because the builders refuse to overwrite an export.
"""
import argparse
import re
import subprocess
import sys
from pathlib import Path
from blockbench_mcp import Client

ROOT=Path(__file__).resolve().parents[1]
MODELS=ROOT/"output/lobby-concept/assets/models"
NPCS={"banker":("npc_arcane_banker_v2","arcane_banker"),"smith":("npc_rune_smith_v2","rune_smith"),
      "warden":("npc_quest_warden_v2","quest_warden"),"mage":("npc_portal_mage_v2","portal_mage")}

def run(*args):
    result=subprocess.run([sys.executable,*args],cwd=ROOT/"tools",capture_output=True,text=True)
    if result.returncode: raise SystemExit(result.stdout[-2000:]+result.stderr[-3000:])
    return result.stdout

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("npc",choices=list(NPCS))
    args=parser.parse_args()
    name,stem=NPCS[args.npc]
    for f in MODELS.glob(name+"*"):
        if f.name==name+".bbmodel" or f.name.startswith(name+"-") or f.name==name+".png": f.unlink()
    client=Client()
    client.call("risky_eval",{"code":"(()=>{for(const p of ModelProject.all.filter(p=>p.name==='"+name+"')){p.select();p.saved=true;p.close(true)}return true})()"})
    text=client.call("create_project",{"name":name,"format":"free"})["content"][0]["text"]
    uuid=re.search(r"UUID: ([0-9a-f-]+)",text).group(1)
    run("build_"+stem+".py","--project-uuid",uuid)
    run("preview_"+stem+".py","--project-uuid",uuid)
    run("manifest_"+stem+".py")
    print(run("verify_"+stem+".py").strip().replace("\n"," "))

if __name__=="__main__": main()
