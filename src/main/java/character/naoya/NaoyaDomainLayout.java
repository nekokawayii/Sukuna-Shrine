package character.naoya;

/**
 * 禅院直哉领域「时胞月宫殿」的内部结构（由用户改好的结构文件烘焙，勿手改）。
 *
 * <p>尺寸 45×9×47；非空气方块 1915 个；状态 0=黑混凝土 1=白混凝土 2=紫水晶块。
 * <p>坐标换算（相对领域圆心）：世界 = 圆心 + (x − 22, y − 1, z − 22)。
 */
final class NaoyaDomainLayout {
    static final int SIZE_X = 45, SIZE_Y = 9, SIZE_Z = 47;
    static final int OFFSET_X = 22, OFFSET_Y = -1, OFFSET_Z = 22;
    static final int COUNT = 1915;
    static final String DATA =
        "eNot2Im2qrgWhWGj4LZFaYTQ961V9f6Pd3/mdWR8B9F9ktAkWSvx4XB4H8zBR4AQET6IkcAiRYYcBZ6H48HDC2/4CBAiwgcx" +
        "ElikyJCjQIkKNe6H0+GBJzy88IaPACEifBAjgUWKDDkKlKhQo0GL28Ghfof6Hep3qN+hfof6Hep3qN+hfof6Hep3qN+hfof6" +
        "Hep3qN+hfof6Hep3qN+hfof6Hep3Dh2uB5d2XNpxacelHZd2XNpxacelHZd2XNpxacelHZd2XNpxacelHZd2XNpxacelHZd2" +
        "XNpxacelHZd23EOPv8P5cMEVN9zxwBMeXnjDR4AQET6IkcAiRYYcBUpUqNGgRYceA0acaX0vF1xxwx0PPOHhhTd8BAgR4YMY" +
        "CSxSZMhRoESFGg1adOgxYMT0u/q9B3u54oY7HnjCwwtv+AgQIsIHMRJYpMiQo0CJCjUatOjQY8CICa7u/n4HrmbvxV5uuOOB" +
        "Jzy88IaPACEifBAjgUWKDDkKlKhQo0GLDj0GjLQ7cZzh6M3b34Sb+dPduKk3e7njgSc8vPCGjwAhInwQI4FFigw5CpSoUKNB" +
        "iw49BoyYaH/muOCkEbePhrs56w256yld1aO7ygNPeHjhDR8BQkT4IEYCixQZchQoUaFGgxYdegwYMWGmHwvH9TfTOBqlD3PW" +
        "m/vQ23PVnXqoZ3t5wsMLb/gIECLCBzESWKTIkKNAiQo1GrTo0GPAiAkz/Vg4rjhqBtxnqadxNaKfGlkXvVFPPUnumtl7txcP" +
        "L7zhI0CICB/ESGCRIkOOAiUq1GjQUm/HsceAERNmLPy2ctx+s/5Js6hnXM04nkb+RW+8pzeNp2oe6p2n8sIbPgKEiPBBjAQW" +
        "KTLkKFCiQo0GLfV2HHsMGDFhxsJvK8fttyqxihhHM/BLs+KfZoWXRiUjwdz1ZF+6g3sP9/KGjwAhInwQI4FFigw5CpSoUKNB" +
        "i476e44DRkyYsWDlt42j0ar51krnaJV4a+b+08y1zxpvc9OoeOsNfOoOvtXLvfgIECLCBzESWKTIkKNAiQo1GrTo0NPOwHHE" +
        "hBkLVmz4/qKDo1ZjX6umq9XF1yx/0ezmm5tGr6+R8tST9nU36anxOQYIEeGDGAksUmTIUfD3JccKNRq06NDz28BxxIQZC1Zs" +
        "+P6imKOihkCru6tVMNBqdNEsHGj2u2s0Bxo1np54YN7qZaASIsIHMRJYpMiQo0DJ/6s41mjQokOPASMmzFiwYsP3F20dFd2E" +
        "ikJcrdahVs2LVotQs/Rds06o0e3pzQzNW3czVE/3EuGDGAksUmTIUaDk/1UcazRo0aHHgBETZixYseH7iwqPisIiRUuuoopI" +
        "q/tFq1qk1eSu2THSLORpBEV6Q33dzUg93csHMRJYpMiQo0CJCjUatOjQY8CICTMWrNjw/UWvR0WLH0V1rqKfj6KQi1bfj1a9" +
        "u2bxj2ZLTyP9o5Hk66l/dEf3nu4lRgKLFBlyFChRoUaDFh16DBgxYcaCFRu+OCjKjhUtnxR9xooOz4qWYkUsV63OsVbDh2b1" +
        "WDPnSyM+1mgK9ORj3dW9txST8K9Figw5CpSoUKNBiw49BoyYMGPBig1f/PPLCo6KwhNFy66iykTR3UVRTaJo4q7VMdEq5GkG" +
        "TTRD+RpNid7USHd172miYpEiQ44CJSrUaNCiQ48BIybMWLBiw/eXvRyVLVhF9a6iX6so9KLoyyrquWsVt1otPc30VjOpr1Fv" +
        "NaIiPX2ru7r3dC8pMuQoUKJCjQYtOvQYMGLCjAUrNnx/WdZRWU2q7MNVlJ4qWr4oSkwVnd0VbaRa1T2tSKlmfF+zU6qRH+kt" +
        "TfX0E/UyVcmQo0CJCjUatOjQY8CICTMWrNjw/WWDR2VfmbIkV9lEpqj+omg2UxR5V1SUKfrwtHJmWpl8zaKZZqhIoykzvKW8" +
        "s5mx6mWmkqNAiQo1GrTo0GPAiAkzFqzY8P1lrUdlibmyOVdZT67s46KoO1e0e1f0litK8rTC51pBfc32uWbSSKM+p685fc2N" +
        "1d3M1dO9FChRoUaDFh16DBgxYcaCFRu+v+z6qGy2UNbpKjsrlCVdlB0UisrvijILRXOeIpFCK72vVanQjB9pdiroa3FkJBmr" +
        "p17oju493UuJCjUatOjQY8CICTMWrNjw/WXxJ2XFpbLWs7K4UpnUVVlDqSj9oWizVET3UiSyr/ClVs9QM/0+g5b0s+Selsbq" +
        "zSz11HP1sFSpUKNBiw49BoyYMGPBiu23y3BS1l4pqz4ry6yU6V2V1VTKIh6KhitFnC9FSpWikOCwr5iVViNmefpY0cfKWI2c" +
        "Sm9lrjtYqYd7qdGgRYceA0ZMmLFgxfbbBTlpV6FW1n9WFlwrE70q66qV5TwUrdeKiF+K5GpFSYFW9FqrJasQfazpY22sRnat" +
        "UZPrCde6g3sP99KgRYceA0ZMmLFgxfbbUXG0G9Foh+BPGXKjDPWm7KtRpvNUpN4oGn4remsUGYVaxffVsaF/Df1rjNWs02hE" +
        "53r7Gj3dSj1rVFp06DFgxIQZC9bfTo+jXZJWOxd/ytxbZc43ZYWtMrA9y/EUobeKgH1Fa60ioUirdkvfWvrWGqvZsNVMk2tU" +
        "tHrrKt2xVj3bS4ceA0ZMmLFg/e0QudpJ6bSjcVEm3ymDvisj3LOuPavplDm8Fel2iiJDRTx7JNHRr45+dcZqhu40++UaqZ1G" +
        "QqWn2Olu7b3aS48BIybMWH47SWftrPTa3bgqk++VOT+UAfbKsl57drBH3Ypme0WKkaKanv709Kc3VqtFr5k416zRa1RWeqN6" +
        "PblWPelVBoyYMP92l/60uzJod+OmzH1QlvxUljcok3of9sh/UFQdKgIcDNEV/Rjox0A/BpNqJRg0yxaaFQaNvFpvzqCn1KkH" +
        "g8qI6be79afdnVG7KzftHIzK0p/KMkdlcm9lHqOi+lAR6B7ZjfRhpA+jsVoxR61GuWbOUTNTpVE16u1t9TRG3YW9B3uZfrtb" +
        "++7Sdd9J0a7FpB2CpzLcSVnkW1nPpIwiVPS7R5UT7U+0Pxmr1XrSSphr1p40K1Ya0ZNGTqs3YdITGNTy/3eSbvsuyb4boYx/" +
        "Vlb9UjY4K9MKlBXMiriJZGlzps3ZWEUHs1beXKvErFm40gwya6S2evNmPfF9h+h+2Hc8Fu0seMqKF2WdvjKkRdlHpEh5oZ2F" +
        "dhZjFYEsWt1zrUSLZvpKs9Si2aDV2/3/nZ59R+V52HcEVmXdb2WJqzKwUNnCHoWv1L9S/2qsoptVkUOuVW7VKlJpBlw107SH" +
        "/++C7LsML2XHmzLPQFnSZshAeCc36tyoc6POjXdyM5lW900rZ6kZf9Os+lbG+lU2GCpz2TOCL///y///GqtI66soJteKu//+" +
        "D7//w+//0Na/fP6Xz//y+T8+/8fn/8zF/Jkregz445u9DBjxoDxRo/mdNTiZt0qBFSfjc/TN0QTUF1BfYDZcTEhdofFMZD6I" +
        "kaDC/s1eYiSwv7/Yv9lLAvv7Hx+dJSr299exPlnVZuGr1d5kyLG3nOKtnq18t9LfVT3LcD+6psXteDYdnse78VChRnB8mQz+" +
        "8W1yHI+h+SBGgg3HY2QuiPBBAosBm377UOfHhIhgkaLCoO9jzmOO3BVYpPrM3UACq88p7aacp/xtyd+U5n3s6RvXigLecaHe" +
        "xbyOqynxPDmmxuPkmgav082UCE5cF6IT14MYCSzCE9cCixTHE31Eig3HU2wu8BAiRYUBG46nhO+5Bmz6zPPB50QuhASW81Tf" +
        "p3xP3oEElvPgVNK3kr529LWj7zN9n7mWhWvh2pyTKeE5jqnwdq7GR44CofMyKWInMJHD9SFGAovQ4boQOlwTAofrQIoMFyeh" +
        "bvqNFBUGHB3Lb5bvuRaHa0GClPMBG45OSptck8M1IYHlfIPvFPSxoD2uD0+npZ2W+ltT4+ZMpsPdmU2LwD2aDG+XkYUcBUL3" +
        "YlL4LqMMGXJErmcsIpdrRowEFqHLM4VFitDl2hG6XDdCl2uF53J9iPBxuUYksJynqHBxuUZ8XK4RCSznA45uZq7oseHkMuJw" +
        "cpklUGDFw61Ng4fLTIIaDf5cZh0MGPV5pL6R+ka+G014PpoIH8RIYJHq+wvfX/j+wvcXvt/PPc49zj0+c0+QIDozVpHAIjwz" +
        "NmGRIjxzXxCeGYewSPWZ+4IEFq8z9wExEpR4nTOO3Idzbt4o0ON6LjgW5nQuzQ0dVjjnytzRYoFzrjmvOa85r417bswM99xy" +
        "bM353JkJf+fejPj8MWMiQfTHbIoYCSyiP+ZTxEhgEf0xfyJGAqtz5lMk+/ESm/8B58ZRnA==";

    private static byte[] bytes;

    /** 解压后的字节：每 4 字节一块（x, y, z, 状态）。 */
    static synchronized byte[] bytes() {
        if (bytes != null) return bytes;
        try {
            byte[] compressed = java.util.Base64.getDecoder().decode(DATA);
            java.util.zip.Inflater inflater = new java.util.zip.Inflater();
            inflater.setInput(compressed);
            byte[] out = new byte[COUNT * 4];
            int offset = 0;
            while (offset < out.length && !inflater.finished()) {
                offset += inflater.inflate(out, offset, out.length - offset);
            }
            inflater.end();
            bytes = out;
        } catch (java.util.zip.DataFormatException exception) {
            throw new IllegalStateException("领域结构损坏", exception);
        }
        return bytes;
    }

    private NaoyaDomainLayout() {}
}
