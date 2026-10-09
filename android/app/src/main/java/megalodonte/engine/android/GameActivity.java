package megalodonte.engine.android;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import megalodonte.engine.*;
import java.util.*;
public final class GameActivity extends Activity {
    private GameView view;
    @Override public void onCreate(Bundle state){super.onCreate(state);try{
        String json;try(var stream=getAssets().open("game.json")){json=new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
        view=new GameView(Codec.read(json));setSafeContent(view);
    }catch(Exception e){android.widget.TextView error=new android.widget.TextView(this);error.setText("Falha ao carregar jogo: "+e.getMessage());setSafeContent(error);}}
    /** Android 15 draws edge-to-edge; keep gameplay and HUD inside system insets. */
    private void setSafeContent(View content) {
        var host = new android.widget.FrameLayout(this);
        host.setBackgroundColor(Color.BLACK);
        host.addView(content, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        host.setOnApplyWindowInsetsListener((root, insets) -> {
            var safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            root.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        setContentView(host);
        host.requestApplyInsets();
    }
    @Override protected void onPause(){super.onPause();if(view!=null){view.active=false;view.dx=view.dy=0;}}
    @Override protected void onResume(){super.onResume();if(view!=null){view.active=true;view.last=0;view.postInvalidateOnAnimation();}}
    private final class GameView extends View {
        final megalodonte.engine.Runtime runtime;final Paint paint=new Paint();final Map<String,Bitmap> images=new HashMap<>();
        boolean active=true;long last;double dx,dy;
        GameView(Game game){super(GameActivity.this);runtime=new megalodonte.engine.Runtime(game);for(var e:runtime.scene.objects)if(!e.assetPath.isBlank())try(var stream=getAssets().open(e.assetPath)){images.put(e.assetPath,BitmapFactory.decodeStream(stream));}catch(Exception ignored){}
            for(var h:runtime.scene.hud)if(!h.assetPath.isBlank())try(var stream=getAssets().open(h.assetPath)){images.put(h.assetPath,BitmapFactory.decodeStream(stream));}catch(Exception ignored){} }
        @Override protected void onDraw(Canvas canvas){long now=System.nanoTime();runtime.update(last==0?0:(now-last)/1e9,dx,dy);last=now;var s=runtime.scene;
            canvas.drawColor(Color.BLACK);float scale=(float)Math.min(getWidth()/s.worldWidth,getHeight()/s.worldHeight),ox=(float)(getWidth()-s.worldWidth*scale)/2,oy=(float)(getHeight()+s.worldHeight*scale)/2;
            paint.setColor(Color.parseColor(s.background));canvas.drawRect(ox,oy-(float)s.worldHeight*scale,ox+(float)s.worldWidth*scale,oy,paint);
            for(var e:s.objects){var pose=Hierarchy.pose(s,e);RectF rect=new RectF(ox+(float)pose.x()*scale,oy-(float)(pose.y()+e.height)*scale,ox+(float)(pose.x()+e.width)*scale,oy-(float)pose.y()*scale);canvas.save();canvas.rotate((float)(pose.rotation()%360),rect.centerX(),rect.centerY());Bitmap image=images.get(e.assetPath);if(image!=null)canvas.drawBitmap(image,null,rect,paint);else{paint.setColor(Color.parseColor(e.color));canvas.drawRect(rect,paint);}canvas.restore();}
            for(var h:s.hud){var b=HudLayout.resolve(h,getWidth(),getHeight());canvas.save();canvas.clipRect((float)b.x(),(float)b.y(),(float)(b.x()+b.width()),(float)(b.y()+b.height()));
                paint.setColor(Color.parseColor(h.color));paint.setAntiAlias(true);
                if(h.type.equals("text")){paint.setTextSize((float)(h.fontSize*b.scale()));canvas.drawText(h.text,(float)b.x(),(float)b.y()-paint.getFontMetrics().top,paint);}
                else{Bitmap image=images.get(h.assetPath);RectF rect=new RectF((float)b.x(),(float)b.y(),(float)(b.x()+b.width()),(float)(b.y()+b.height()));if(image!=null)canvas.drawBitmap(image,null,rect,paint);else canvas.drawRect(rect,paint);}
                canvas.restore();}
            if(active)postInvalidateOnAnimation();
        }
        @Override public boolean onTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){dx=dy=0;return true;}dx=event.getX()<getWidth()/3f?-1:event.getX()>getWidth()*2/3f?1:0;dy=event.getY()<getHeight()/3f?1:event.getY()>getHeight()*2/3f?-1:0;return true;}
    }
}
