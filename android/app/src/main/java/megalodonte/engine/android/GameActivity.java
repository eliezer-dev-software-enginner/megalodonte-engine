package megalodonte.engine.android;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.media.MediaPlayer;
import megalodonte.engine.*;
import java.util.*;

public final class GameActivity extends Activity {
    private GameView view;
    @Override public void onCreate(Bundle state){super.onCreate(state);try{
        String json;try(var stream=getAssets().open("game.json")){json=new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
        view=new GameView(Codec.read(json));setSafeContent(view);
    }catch(Exception e){android.widget.TextView error=new android.widget.TextView(this);error.setText("Falha ao carregar jogo: "+e.getMessage());setSafeContent(error);}}
    private void setSafeContent(View content){
        var host=new android.widget.FrameLayout(this);host.setBackgroundColor(Color.BLACK);
        host.addView(content,new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        host.setOnApplyWindowInsetsListener((root,insets)->{var safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());root.setPadding(safe.left,safe.top,safe.right,safe.bottom);return insets;});setContentView(host);host.requestApplyInsets();host.post(()->{var controller=host.getWindowInsetsController();if(controller!=null){controller.hide(WindowInsets.Type.statusBars());controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}});
    }
    @Override protected void onPause(){super.onPause();if(view!=null){view.active=false;view.clearInput();if(view.music!=null)view.music.pause();}}
    @Override protected void onResume(){super.onResume();if(view!=null){view.active=true;view.last=0;if(view.music!=null&&!view.runtime.paused)view.music.start();view.postInvalidateOnAnimation();}}
    @Override protected void onDestroy(){if(view!=null)view.close();super.onDestroy();}
    private final class GameView extends View {
        final megalodonte.engine.Runtime runtime;
        final Paint paint=new Paint();
        final Map<String,Bitmap> images=new HashMap<>();
        final Map<Integer,String> touchActions=new HashMap<>();
        final Set<String> held=new HashSet<>();
        final List<MediaPlayer> effects=new ArrayList<>();
        MediaPlayer music;String musicScene="";
        boolean muted;boolean audioPaused;boolean active=true;long last;double dx,dy;
        GameView(Game game){super(GameActivity.this);paint.setFilterBitmap(false);
            runtime=new megalodonte.engine.Runtime(game,megalodonte.engine.Runtime.reflection(getClass().getClassLoader()),new megalodonte.engine.Runtime.Events(){
                public java.util.Map<String,String> loadState(){try{var type=new com.google.gson.reflect.TypeToken<java.util.Map<String,String>>(){}.getType();java.util.Map<String,String> state=Codec.GSON.fromJson(getSharedPreferences("game-state",0).getString("state","{}"),type);return state==null?java.util.Map.of():state;}catch(Exception e){return java.util.Map.of();}}
                public void saveState(java.util.Map<String,String> state){getSharedPreferences("game-state",0).edit().putString("state",Codec.GSON.toJson(state)).apply();}
                public void log(String message){android.util.Log.i("MegalodonteScript",message);}
                public void playSound(String asset){sound(asset);}
                public void quit(){GameActivity.this.finish();}
                public void muteAudio(boolean value){muted=value;if(music!=null){float v=value?0:(float)runtime.scene.musicVolume;music.setVolume(v,v);}for(var player:effects)player.setVolume(value?0:1,value?0:1);}
                public void pauseAudio(boolean value){audioPaused=value;if(music!=null){if(value)music.pause();else music.start();}for(var player:effects){if(value)player.pause();else player.start();}}
            });
            for(var scene:runtime.game.scenes){for(var e:scene.objects){loadImage(e.assetPath);for(var animation:e.animations.values())for(var frame:animation.frames)loadImage(frame);}for(var h:scene.hud)loadImage(h.assetPath);for(var map:scene.tileMaps)for(var tile:map.tiles.values())loadImage(tile.assetPath);}
        }
        void loadImage(String path){if(path.isBlank()||images.containsKey(path))return;try(var stream=getAssets().open(path)){Bitmap bitmap=BitmapFactory.decodeStream(stream);if(bitmap!=null)images.put(path,bitmap);}catch(Exception e){android.util.Log.w("Megalodonte",path,e);}}
        MediaPlayer audio(String asset)throws Exception{MediaPlayer player=new MediaPlayer();try(var fd=getAssets().openFd(asset)){player.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());player.prepare();}catch(Exception e){player.release();throw e;}return player;}
        void sound(String asset){if(muted||audioPaused)return;try{MediaPlayer player=audio(asset);effects.add(player);player.setOnCompletionListener(done->{effects.remove(done);done.release();});player.start();}catch(Exception e){android.util.Log.e("MegalodonteAudio",asset,e);}}
        void updateMusic(){if(music!=null){float v=muted?0:(float)runtime.scene.musicVolume;music.setVolume(v,v);}String key=MusicTrack.key(runtime.scene);if(key.equals(musicScene))return;if(music!=null){music.release();music=null;}musicScene=key;
            if(runtime.scene.musicAssetPath.isBlank())return;try{music=audio(runtime.scene.musicAssetPath);music.setLooping(runtime.scene.musicLoop);float volume=muted?0:(float)runtime.scene.musicVolume;music.setVolume(volume,volume);if(!audioPaused)music.start();}catch(Exception e){android.util.Log.e("MegalodonteAudio",runtime.scene.musicAssetPath,e);}}
        @Override protected void onDraw(Canvas canvas){
            if(!active)return;
            long now=System.nanoTime();try{runtime.update(last==0?0:(now-last)/1e9,dx,dy);updateMusic();}catch(Exception e){active=false;clearInput();android.widget.Toast.makeText(GameActivity.this,"Erro no script: "+e.getMessage(),android.widget.Toast.LENGTH_LONG).show();return;}last=now;
            var scene=runtime.scene;var view=CameraView.of(scene.camera,getWidth(),getHeight());float scale=(float)view.scale(),ox=(float)view.originX(),oy=(float)view.originY();
            canvas.drawColor(Color.BLACK);canvas.save();canvas.clipRect((float)view.screenX(view.left()),(float)view.screenY(view.bottom()+view.height()),(float)view.screenX(view.left()+view.width()),(float)view.screenY(view.bottom()));paint.setColor(Color.parseColor(scene.background));paint.setAlpha(255);canvas.drawRect(ox,oy-(float)scene.worldHeight*scale,ox+(float)scene.worldWidth*scale,oy,paint);
            TileDraw.render(scene,view,(tile,x,y,w,h,alpha,a,b,c,d)->{Bitmap image=images.get(tile.assetPath);if(image==null)return;canvas.save();float left=(float)Math.floor(x),top=(float)Math.floor(y),right=(float)Math.ceil(x+w),bottom=(float)Math.ceil(y+h);canvas.translate((left+right)/2,(top+bottom)/2);canvas.scale(right-left,bottom-top);Matrix matrix=new Matrix();matrix.setValues(new float[]{(float)a,(float)c,0,(float)b,(float)d,0,0,0,1});canvas.concat(matrix);paint.setAlpha((int)(alpha*255));canvas.drawBitmap(image,new Rect(tile.sourceX,tile.sourceY,tile.sourceX+tile.width,tile.sourceY+tile.height),new RectF(-.5f,-.5f,.5f,.5f),paint);canvas.restore();});paint.setAlpha(255);
            for(var e:scene.objects){if(!e.visible)continue;var pose=Hierarchy.pose(scene,e);RectF rect=new RectF(ox+(float)pose.x()*scale,oy-(float)(pose.y()+e.height)*scale,ox+(float)(pose.x()+e.width)*scale,oy-(float)pose.y()*scale);canvas.save();canvas.rotate((float)(pose.rotation()%360),rect.centerX(),rect.centerY());if(e.flipX)canvas.scale(-1,1,rect.centerX(),rect.centerY());Bitmap image=images.get(Sprites.asset(e));if(image!=null)canvas.drawBitmap(image,null,rect,paint);else{paint.setColor(Color.parseColor(e.color));canvas.drawRect(rect,paint);}canvas.restore();}
            canvas.restore();
            for(var hud:scene.hud){if(!hud.visible)continue;loadImage(hud.assetPath);var b=HudLayout.resolve(hud,getWidth(),getHeight());canvas.save();canvas.clipRect((float)b.x(),(float)b.y(),(float)(b.x()+b.width()),(float)(b.y()+b.height()));
                RectF rect=new RectF((float)b.x(),(float)b.y(),(float)(b.x()+b.width()),(float)(b.y()+b.height()));
                if(hud.type.equals("button")&&hud.assetPath.isBlank()){paint.setColor(Color.rgb(36,88,120));canvas.drawRoundRect(rect,8,8,paint);}
                if(hud.type.equals("button")&&!hud.assetPath.isBlank()){Bitmap button=images.get(hud.assetPath);if(button!=null)canvas.drawBitmap(button,null,rect,paint);}
                paint.setColor(Color.parseColor(hud.color));
                if(hud.type.equals("text")||hud.type.equals("button")){paint.setAntiAlias(true);paint.setTextSize((float)(hud.fontSize*b.scale()));canvas.drawText(hud.text,(float)b.x(),(float)b.y()-paint.getFontMetrics().top,paint);paint.setAntiAlias(false);}
                else{Bitmap image=images.get(hud.assetPath);if(image!=null)canvas.drawBitmap(image,null,rect,paint);else canvas.drawRect(rect,paint);}canvas.restore();
            }
            if(runtime.fadeAlpha()>0){paint.setColor(Color.argb((int)(runtime.fadeAlpha()*255),0,0,0));canvas.drawRect(0,0,getWidth(),getHeight(),paint);}
            postInvalidateOnAnimation();
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            if(event.getActionMasked()==MotionEvent.ACTION_CANCEL){clearInput();return true;}
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN||event.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN){int i=event.getActionIndex();String action="";
                for(int j=runtime.scene.hud.size()-1;j>=0;j--){var h=runtime.scene.hud.get(j);if(h.visible&&h.type.equals("button")&&HudLayout.resolve(h,getWidth(),getHeight()).contains(event.getX(i),event.getY(i))){action=h.action.isBlank()?h.id:h.action;break;}}
                touchActions.put(event.getPointerId(i),action);
            }
            int up=(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_POINTER_UP)?event.getActionIndex():-1;
            if(up>=0)touchActions.remove(event.getPointerId(up));
            Set<String> next=new HashSet<>(touchActions.values());next.remove("");for(String action:held)if(!next.contains(action))runtime.setAction(action,false);for(String action:next)runtime.setAction(action,true);held.clear();held.addAll(next);
            dx=dy=0;
            for(int i=0;i<event.getPointerCount();i++)if(i!=up&&"".equals(touchActions.get(event.getPointerId(i)))){dx=event.getX(i)<getWidth()/3f?-1:event.getX(i)>getWidth()*2/3f?1:0;dy=event.getY(i)<getHeight()/3f?1:event.getY(i)>getHeight()*2/3f?-1:0;break;}
            return true;
        }
        void clearInput(){for(String action:held)runtime.setAction(action,false);held.clear();touchActions.clear();dx=dy=0;}
        void close(){active=false;runtime.close();if(music!=null){music.release();music=null;}for(var effect:effects)effect.release();effects.clear();for(var image:images.values())image.recycle();images.clear();}
    }
}
