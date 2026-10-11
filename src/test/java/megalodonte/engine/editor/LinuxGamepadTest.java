package megalodonte.engine.editor;
import com.sun.jna.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class LinuxGamepadTest {
    @Test void nativeBackendLoadsAndClosesFileDescriptors(){
        Assumptions.assumeTrue(System.getProperty("os.name").startsWith("Linux"));
        var libc=Native.load("c",LinuxGamepad.LibC.class);int fd=libc.open("/dev/null",0x800);assertTrue(fd>=0);
        try{assertEquals(-1,libc.ioctl(fd,new NativeLong(0x80406a32L),new byte[64]));}finally{assertEquals(0,libc.close(fd));}
    }
}
