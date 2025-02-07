package com.discord.bot.maple.bots;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Random;

public class MessageReceiveListener extends ListenerAdapter {


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (event.getAuthor().isBot()) {
            return;
        }

        String msg = event.getMessage().getContentDisplay();
        if (!msg.contains("!")) {
            return;
        }
        Util util = Util.getInstance();

        System.out.printf("[%s] %#s: %s\n",
                event.getChannel(),
                event.getAuthor(),
                msg);

        // 받은 메세지 내용이 !ping이라면
        switch(msg) {
            case "!명령어":
                event.getChannel().sendMessage("```\n" +
                        "!아케인\n" +
                        "!어센틱\n" +
                        "!무토\n" +
                        "!5퍼\n" +
                        "!렙반감\n" +
                        "!포뻥\n" +
                        "!메소반감\n" +
                        "!경험치반감\n" +
                        "/조각계산\n" +
                        "!칠흑\n" +
                        "!기대값\n" +
                        "!헤영지\n" +
                        "!유틸\n" +
                        "!공략\n" +
                        "!도핑\n" +
                        "```").queue();
                break;
            case "!ping" :
                event.getChannel().sendMessage("Pong!").queue();
                break;
            case "!아케인" :
                event.getChannel().sendMessage("```\n" +
                        "Arcane Symbol/claim\n" +
                        "```").queue();
                break;
            case "!어센틱" :
                event.getChannel().sendMessage("```\n" +
                        "Sacred Symbol/claim\n" +
                        "```").queue();
                break;
            case "!무토":
                try {
                    File file = util.getFile("메이플_무토_레시피.png");
                    event.getChannel().sendMessage("무토 레시피")
                            .addFiles(FileUpload.fromData(file))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!5퍼":
                try {
                    File file = util.getFile("5percent.jpeg");
                    event.getChannel().sendMessage("보스 5퍼")
                            .addFiles(FileUpload.fromData(file))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!렙반감":
                try {
                    File file = util.getFile("lvl_decrease.jpg");
                    event.getChannel().sendMessage("렙반감")
                            .addFiles(FileUpload.fromData(file))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!포뻥":
                try {
                    File file1 = util.getFile("arcane_bbung.png");
                    File file2 = util.getFile("sacred_bbung.png");
                    event.getChannel().sendMessage("보스별 포스 및 포뻥")
                            .addFiles(FileUpload.fromData(file1))
                            .addFiles(FileUpload.fromData(file2))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!메소반감":
                try {
                    File file = util.getFile("meso_decrease.png");
                    event.getChannel().sendMessage("메소반감")
                            .addFiles(FileUpload.fromData(file))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!경험치반감":
                try {
                    File file = util.getFile("exp_decrease.png");
                    event.getChannel().sendMessage("경험치반감")
                            .addFiles(FileUpload.fromData(file))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!도핑":
                try {
                    File file = util.getFile("doping.png");
                    event.getChannel().sendMessage("GMS 도핑목록")
                            .addFiles(FileUpload.fromData(file))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!칠흑":
                try {
                    int random = (int) (Math.random() * 100) + 1;
                    event.getChannel().sendMessage("오늘 칠흑 먹을 확률은 **" + random + "%** 입니다.")
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!기대값":
                try {
                    File file1 = util.getFile("star150.png");
                    File file2 = util.getFile("star160.png");
                    File file3 = util.getFile("star200.png");
                    File file4 = util.getFile("star250.png");
                    event.getChannel().sendMessage("스타포스 기대값")
                            .addFiles(FileUpload.fromData(file1))
                            .addFiles(FileUpload.fromData(file2))
                            .addFiles(FileUpload.fromData(file3))
                            .addFiles(FileUpload.fromData(file4))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!헤영지" :
                event.getChannel().sendMessage("" +
                        "**헤이븐 (아래목록이 없으면 좋음)**\n" +
                        "```\n" +
                        "> Haven: Collect Red Antenna Piece (Hunterizer Red) \n" +
                        "> Haven: Collect Blue Chipsest Piece (Modded Laseroid) \n" +
                        "> Haven: Defeat Hunterizer Red \n" +
                        "> Haven: Defeat Inner Guard EX \n" +
                        "> Scrapyard: Defeat Hunterizer Blue \n" +
                        "> Scrapyard: Deliver Blue Antenna Piece \n" +
                        "> Skyline: Defeat Modded Buffroid \n" +
                        "> Skyline: Free Modded Deliveroid \n" +
                        "> Skyline: Deliver Prison Key Piece \n" +
                        "> Black Heaven Inside: Defeat Scrap Xenoroid DX \n" +
                        "> Black Heaven Inside: Defeat Steel Xenoroid EX \n" +
                        "> Black Heaven Inside: Collect Internal Siren \n" +
                        "> Black Heaven Inside: Collect Steel ID Plate \n" +
                        "> Black Heaven Inside: Deliver Scrap Xenoroid Chipset \n" +
                        "```\n" +
                        "**야영지**\n" +
                        "```\n" +
                        "최대한 길 중복되게 받기" +
                        "```\n"
                ).queue();
                break;
            case "!유틸" :
                event.getChannel().sendMessage("" +
                        ">>> # 유틸 사이트 \n" +
                        "- [GMS 캐릭터검색](<https://maplestory.gg/>) \n" +
                        "- [각종 기대값 계산](<https://brendonmay.github.io/>) \n" +
                        "- [사냥터 빌드짜는 사이트](<https://maplemaps.net/>) \n" +
                        "- [닌자성 도우미](<https://godchangsub.github.io/>) \n" +
                        ""
                ).queue();
                break;
            case "!공략" :
                event.getChannel().sendMessage("" +
                        ">>> # 공략 모음 \n" +
                        "- [퍼밀리어](<https://gall.dcinside.com/mgallery/board/view/?id=globalms&no=10426>) \n" +
                        "- [팬텀 포레스트 (종결표창)](<https://gall.dcinside.com/mgallery/board/view/?id=globalms&no=12096>) \n" +
                        "- [스텔라 탐정단 (무적링/크뎀링)](<https://gall.dcinside.com/mgallery/board/view/?id=heroic&no=1226>) \n" +
                        "- [커머시 (160제 얼장/눈장)](<https://gall.dcinside.com/mgallery/board/view/?id=globalms&no=15393>) \n" +
                        "- [닌자성 (펫버프 달려있는 공짜펫)](<https://gall.dcinside.com/mgallery/board/view/?id=globalms&no=9559>) \n" +
                        "- [애프터랜드 (스펙 토템)](<https://youtu.be/CZ2jB3h6xjc>) \n" +
                        "- [버섯신사 (에스크 선행퀘)](<https://youtu.be/Y_GkulrmB8Y>) \n" +
                        "- [에스크 (재획비/광물)](<https://gall.dcinside.com/mgallery/board/view/?id=globalms&no=7100>) \n" +
                        "- [노히메 (보조/140반지)](<https://youtu.be/zXKJYay-zSI>) \n" +
                        "- [골럭스 (종결 장신구)](<https://gall.dcinside.com/mgallery/board/view/?id=globalms&no=31564>) \n" +
                        "- [멀티펫 하는법](<https://gall.dcinside.com/mgallery/board/view/?id=heroic&no=1777>) \n" +
                        ""
                ).queue();
                break;
        }
    }
}
