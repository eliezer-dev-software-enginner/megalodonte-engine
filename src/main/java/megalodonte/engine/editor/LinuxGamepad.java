package megalodonte.engine.editor;
import com.sun.jna.*;
import java.nio.*;
import java.nio.file.*;
import megalodonte.engine.GamepadInput;
/** Nonblocking Linux joystick reader, using kernel axis/button maps. */
final class LinuxGamepad implements AutoCloseable {
    interface LibC extends Library {int open(String path,int flags);int close(int fd);int read(int fd,byte[] bytes,int size);int ioctl(int fd,NativeLong request,byte[] value);}
    private LibC libc;
    private int fd=-1;
    private long nextScan;
    private final byte[] axes=new byte[64],buttons=new byte[1024],event=new byte[8];
    private double x,y,hx,hy;
    final GamepadInput input=new GamepadInput();
    void poll(boolean active){
        if(!System.getProperty("os.name").startsWith("Linux"))return;
        try{
            if(libc==null)libc=Native.load("c",LibC.class);
            if(fd<0){if(System.nanoTime()<nextScan)return;nextScan=System.nanoTime()+1_000_000_000L;
                for(int i=0;i<16;i++){String path="/dev/input/js"+i;if(!Files.exists(Path.of(path)))continue;fd=libc.open(path,0x800);if(fd<0)continue;
                    if(libc.ioctl(fd,new NativeLong(0x80406a32L),axes)<0||libc.ioctl(fd,new NativeLong(0x84006a34L),buttons)<0){close();continue;}break;}
            }
            if(fd<0)return;
            int n;
            while((n=libc.read(fd,event,8))==8){var buffer=ByteBuffer.wrap(event).order(ByteOrder.nativeOrder());double value=buffer.getShort(4)/32767.0;int type=event[6]&0x7f,index=event[7]&0xff;
                if(type==2&&index<axes.length){switch(axes[index]&0xff){case 0->x=value;case 1->y=value;case 16->hx=value;case 17->hy=value;}}
                else if(type==1&&index*2+1<buttons.length){int code=ByteBuffer.wrap(buttons).order(ByteOrder.nativeOrder()).getShort(index*2)&0xffff;String name=switch(code){case 0x130->"a";case 0x131->"b";case 0x13b->"start";case 0x220->"up";case 0x221->"down";case 0x222->"left";case 0x223->"right";default->"";};if(!name.isEmpty())input.button(name,event[4]!=0);}
            }
            if(n==0||(n<0&&Native.getLastError()!=11)){close();return;}
            if(active)input.axes(x,y,hx,hy);else input.clear();
        }catch(Throwable e){close();nextScan=System.nanoTime()+5_000_000_000L;}
    }
    @Override public void close(){if(fd>=0&&libc!=null)libc.close(fd);fd=-1;x=y=hx=hy=0;input.clear();}
}
