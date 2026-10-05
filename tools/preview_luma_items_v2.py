"""Capture every display context of an item v2 from Blockbench's display mode into one labelled sheet."""
import argparse
import base64
import io
import json
from PIL import Image, ImageDraw
from blockbench_mcp import Client
from build_moonfall_boss import OUT, data
from build_luma_items_v2 import ITEMS as ITEM_V2
from build_luma_props import ITEMS as PROP_ITEMS

ITEMS={**ITEM_V2,**PROP_ITEMS}

LOADERS={"thirdperson_righthand":"loadThirdRight","thirdperson_lefthand":"loadThirdLeft",
         "firstperson_righthand":"loadFirstRight","firstperson_lefthand":"loadFirstLeft",
         "gui":"loadGUI","head":"loadHead","ground":"loadGround","fixed":"loadFixed"}

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("item",choices=list(ITEMS))
    parser.add_argument("--project-uuid",required=True)
    args=parser.parse_args()
    name=ITEMS[args.item][0]
    client=Client()
    def call(tool,arguments):
        project=data(client.call("get_project_info",{}))["project"]
        if project["uuid"]!=args.project_uuid or project["name"]!=name: raise RuntimeError("Active project changed")
        return client.call(tool,arguments)
    call("risky_eval",{"code":"(()=>{Modes.options.display.select();return Modes.selected.id})()"})
    tiles=[]
    try:
        for slot,loader in LOADERS.items():
            call("risky_eval",{"code":"(()=>{DisplayMode."+loader+"();return true})()"})
            image=next(b for b in call("capture_screenshot",{"view":"active","format":"png","max_size":600})["content"] if b.get("type")=="image")
            tile=Image.open(io.BytesIO(base64.b64decode(image["data"]))).convert("RGB")
            if slot=="gui":
                # The GUI slot renders tiny in a large canvas; enlarge the centre so the icon is reviewable.
                w,h=tile.size; tile=tile.crop((w//2-90,h//2-90,w//2+90,h//2+90)).resize((h,h),Image.NEAREST)
            ImageDraw.Draw(tile).text((10,10),slot,fill=(255,255,255))
            tiles.append(tile)
    finally:
        call("set_mode",{"mode_id":"edit"})
    width=max(t.width for t in tiles); height=max(t.height for t in tiles)
    sheet=Image.new("RGB",(width*4,height*2))
    for i,tile in enumerate(tiles): sheet.paste(tile,((i%4)*width,(i//4)*height))
    sheet.save(OUT/(name+"-display.png"))
    print(json.dumps({"display_sheet":str(OUT/(name+"-display.png")),"contexts":list(LOADERS)}))

if __name__=="__main__": main()
