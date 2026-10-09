package megalodonte.engine;
/** Tiles in screen space with normalized flip matrices; clips by camera bounds. */
public final class TileDraw {
    private TileDraw(){}
    public interface Sink {void draw(Game.Tile tile,double x,double y,double width,double height,double alpha,double a,double b,double c,double d);}
    public static void render(Game.Scene scene,CameraView view,Sink sink){
        for(var map:scene.tileMaps){double unit=map.unitScale,tw=map.tileWidth*unit,th=map.tileHeight*unit;
            for(var layer:map.layers){double ox=map.x+layer.offsetX*unit,oy=map.y-layer.offsetY*unit;
                int minX=Math.max(0,(int)Math.floor((view.left()-ox)/tw)-1),maxX=Math.min(map.columns-1,(int)Math.ceil((view.left()+view.width()-ox)/tw)+1);
                int minY=Math.max(0,(int)Math.floor((view.bottom()-oy)/th)-1),maxY=Math.min(map.rows-1,(int)Math.ceil((view.bottom()+view.height()-oy)/th)+1);
                for(int row=minY;row<=maxY;row++)for(int column=minX;column<=maxX;column++){
                    long gid=layer.data[(map.rows-1-row)*map.columns+column];var tile=map.tiles.get((int)(gid&0x0fffffffL));if(tile==null)continue;
                    double h=(gid&0x80000000L)!=0?-1:1,v=(gid&0x40000000L)!=0?-1:1;boolean diagonal=(gid&0x20000000L)!=0;
                    double width=tile.width*unit*view.scale(),height=tile.height*unit*view.scale();
                    sink.draw(tile,view.screenX(ox+column*tw),view.screenY(oy+row*th)-height,width,height,layer.opacity,
                        diagonal?0:h,diagonal?v:0,diagonal?h:0,diagonal?0:v);
                }
            }
        }
    }
}
