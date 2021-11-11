//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import dev.xdark.clientapi.network.Packet;
import dev.xdark.feder.Recyclable;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface Ws extends Packet, Recyclable {
    void read(ByteBuf var1);

    void write(ByteBuf var1);

    default void a(ByteBuf var1) {
        this.read(var1);
    }

//    void a(T var1);

//    default Executor a(T var1) {
//        return var1.executor();
//    }

    default void a(ChannelHandlerContext var1) {
    }

//    default void b(T var1) {
//        this.a(var1);
//    }

    default Class a() {
        return this.getClass();
    }

    default void recycle() {
    }

//    default void processPacket(NetHandler var1) {
//        Wm var2;
//        if (var1 instanceof bD) {
//            var2 = ((bD)var1).a();
//        } else {
//            var2 = (Wm)var1;
//        }
//
//        this.b(var2);
//    }
}
