package megalodonte.engine.editor;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import megalodonte.engine.*;
import java.util.function.Function;

/** Desktop game rendering without editor overlays. */
final class GamePainter {
    static void draw(Canvas canvas,megalodonte.engine.Runtime runtime,Function<String,Image> images){
        var scene=runtime.scene;var g=canvas.getGraphicsContext2D();g.setImageSmoothing(false);
        g.setFill(Color.BLACK);g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        var view=CameraView.of(scene.camera,canvas.getWidth(),canvas.getHeight());double scale=view.scale();
        g.save();g.beginPath();g.rect(view.screenX(view.left()),view.screenY(view.bottom()+view.height()),view.width()*scale,view.height()*scale);g.clip();
        g.setFill(Color.web(scene.background));g.fillRect(view.screenX(0),view.screenY(scene.worldHeight),scene.worldWidth*scale,scene.worldHeight*scale);
        TileDraw.render(scene,view,(tile,x,y,w,h,alpha,a,b,c,d)->{
            var image=images.apply(tile.assetPath);if(image==null)return;g.save();g.setGlobalAlpha(alpha);g.translate(x+w/2,y+h/2);g.scale(w,h);g.transform(a,b,c,d,0,0);g.drawImage(image,tile.sourceX,tile.sourceY,tile.width,tile.height,-.5,-.5,1,1);g.restore();
        });
        for(var entity:scene.objects){if(!entity.visible)continue;var pose=Hierarchy.pose(scene,entity);
            double x=view.screenX(pose.x()),y=view.screenY(pose.y()+entity.height),w=entity.width*scale,h=entity.height*scale;
            if(x+w<0||x>canvas.getWidth()||y+h<0||y>canvas.getHeight())continue;
            g.save();g.translate(x+w/2,y+h/2);g.rotate(pose.rotation());if(entity.flipX)g.scale(-1,1);
            var image=images.apply(Sprites.asset(entity));if(image!=null)g.drawImage(image,-w/2,-h/2,w,h);else{g.setFill(Color.web(entity.color));g.fillRect(-w/2,-h/2,w,h);}g.restore();
        }g.restore();
        for(var hud:scene.hud){if(!hud.visible)continue;var box=HudLayout.resolve(hud,canvas.getWidth(),canvas.getHeight());g.save();g.beginPath();g.rect(box.x(),box.y(),box.width(),box.height());g.clip();
            var image=images.apply(HudPresentation.asset(hud));
            if(image!=null)g.drawImage(image,box.x(),box.y(),box.width(),box.height());
            else if(hud.type.equals("button")){g.setFill(Color.web("#245878"));g.fillRoundRect(box.x(),box.y(),box.width(),box.height(),8,8);}
            if(!hud.type.equals("image")){g.setFill(Color.web(hud.color));g.setFont(Font.font("Monospaced",hud.fontSize*box.scale()));g.setTextBaseline(javafx.geometry.VPos.TOP);g.fillText(hud.text,box.x()+4*box.scale(),box.y()+4*box.scale());}
            g.restore();
        }
        if(runtime.fadeAlpha()>0){g.setFill(Color.color(0,0,0,runtime.fadeAlpha()));g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());}
    }
}
