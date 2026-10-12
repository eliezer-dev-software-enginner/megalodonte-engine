package megalodonte.engine.editor;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import megalodonte.engine.*;
import java.util.function.Function;

/** Desktop game rendering without editor overlays. */
final class GamePainter {
    private static final SpriteFrameCache<Image> frames=new SpriteFrameCache<>();
    static void draw(Canvas canvas,megalodonte.engine.Runtime runtime,Function<String,Image> images){
        var scene=runtime.scene;var g=canvas.getGraphicsContext2D();g.setImageSmoothing(false);
        g.setFill(Color.BLACK);g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        var view=CameraView.of(scene.camera,canvas.getWidth(),canvas.getHeight());double scale=view.scale();
        g.save();g.beginPath();g.rect(view.screenX(view.left()),view.screenY(view.bottom()+view.height()),view.width()*scale,view.height()*scale);g.clip();
        g.setFill(Color.web(scene.background));g.fillRect(view.screenX(0),view.screenY(scene.worldHeight),scene.worldWidth*scale,scene.worldHeight*scale);
        BackgroundDraw.render(scene,view,(asset,x,y,w,h)->{var image=images.apply(asset);if(image!=null)g.drawImage(image,x,y,w,h);});
        TileDraw.render(scene,view,(tile,x,y,w,h,alpha,a,b,c,d)->{
            var image=images.apply(tile.assetPath);if(image==null)return;g.save();g.setGlobalAlpha(alpha);g.translate(x+w/2,y+h/2);g.scale(w,h);g.transform(a,b,c,d,0,0);g.drawImage(image,tile.sourceX,tile.sourceY,tile.width,tile.height,-.5,-.5,1,1);g.restore();
        });
        var visible=DrawOrder.visibleEntities(scene,view);frames.beginFrame(visible);
        for(var entity:visible){if(!entity.visible)continue;var pose=Hierarchy.pose(scene,entity);
            double x=view.screenX(pose.x()),y=view.screenY(pose.y()+entity.height),w=entity.width*scale,h=entity.height*scale;

            g.save();g.setGlobalAlpha(entity.opacity);g.translate(x+w/2,y+h/2);g.rotate(pose.rotation());if(entity.flipX)g.scale(-1,1);
            var requested=images.apply(Sprites.asset(entity));var image=frames.select(entity,requested!=null&&requested.getProgress()>=1&&!requested.isError()&&requested.getWidth()>0?requested:null);if(image!=null)g.drawImage(image,-w/2,-h/2,w,h);else if(Sprites.asset(entity).isBlank()){g.setFill(Color.web(entity.color));g.fillRect(-w/2,-h/2,w,h);}g.restore();
        }g.restore();
        for(var hud:scene.hud){if(!hud.visible)continue;var box=HudLayout.resolve(hud,canvas.getWidth(),canvas.getHeight());g.save();g.beginPath();g.rect(box.x(),box.y(),box.width(),box.height());g.clip();
            var image=images.apply(HudPresentation.asset(hud));
            if(image!=null)g.drawImage(image,box.x(),box.y(),box.width(),box.height());
            else if(hud.type.equals("button")){g.setFill(Color.web(hud.buttonBackground,hud.buttonOpacity));g.fillRoundRect(box.x(),box.y(),box.width(),box.height(),hud.cornerRadius*box.scale(),hud.cornerRadius*box.scale());}
            if(!hud.type.equals("image")){g.setFill(Color.web(hud.color));g.setFont(Font.font(hud.fontFamily.isBlank()?"Monospaced":hud.fontFamily,hud.fontSize*box.scale()));g.setTextBaseline(javafx.geometry.VPos.TOP);var lines=hud.text.split("\\R",-1);double lineHeight=hud.fontSize*box.scale()*1.2,remaining=box.height()-lineHeight*lines.length;double lineY=box.y()+switch(hud.textVerticalAlign){case "center"->remaining/2;case "bottom"->remaining;default->4*box.scale();};g.setTextAlign(javafx.scene.text.TextAlignment.valueOf(hud.textAlign.toUpperCase(java.util.Locale.ROOT)));double textX=box.x()+switch(hud.textAlign){case "center"->box.width()/2;case "right"->box.width()-4*box.scale();default->4*box.scale();};for(String line:lines){g.fillText(line,textX,lineY);lineY+=lineHeight;}}
            if(hud.focused){g.setStroke(Color.web("#00ff00"));g.setLineWidth(3);g.strokeRect(box.x()+2,box.y()+2,box.width()-4,box.height()-4);}
            g.restore();
        }
        if(runtime.fadeAlpha()>0){g.setFill(Color.color(0,0,0,runtime.fadeAlpha()));g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());}
    }
}
