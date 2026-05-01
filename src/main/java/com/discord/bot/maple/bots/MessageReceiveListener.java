package com.discord.bot.maple.bots;

import com.discord.bot.maple.bots.exp.ExpTrain;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Random;

public class MessageReceiveListener extends ListenerAdapter {

    List<ExpTrain> expTrain = ExpTrain.getInstance();


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
            case "!명령어", "!도움", "!help":
                event.getChannel().sendMessage("```\n" +
                        "!아케인\n" +
                        "!어센틱\n" +
                        "!조각\n" +
                        "!무토\n" +
                        "!5퍼\n" +
                        "!렙반감 or !레벨반감\n" +
                        "!포뻥 or !포스\n" +
                        "!메소반감\n" +
                        "!경험치반감\n" +
                        //"/성장의비약\n" +
                        "!칠흑\n" +
                        "!기대값 or !기댓값\n" +
                        "!헤영지\n" +
                        "!에스페라 or !에페\n" +
                        "!영끌 or !영끌도핑\n" +
                        "!유틸\n" +
                        "!공략\n" +
                        "!도핑\n" +
                        "!시아\n" +
                        "!묵현\n" +
                        "!실\n" +
                        "!추옵\n" +
                        "!연뿌\n" +
                        "!반상\n" +
                        "!몬파\n" +
                        "/연뿌등록\n" +
                        "/연뿌초기화\n" +
                        "/조각계산\n" +
                        "/필요소재비계산\n" +
                        "/보스일정\n" +
                        "/보스일정결과\n" +
                        "/보스일정초기화\n" +
                        "/보스일정확정\n" +
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
            case "!조각" :
                event.getChannel().sendMessage("```\n" +
                        "Sol Erda Fragment\n" +
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
            case "!연뿌":
                if (expTrain.size() == 0) {
                    event.getChannel().sendMessage("등록된 연뿌가 없습니다.").queue();
                    break;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("```") // 코드블럭 시작
                        .append("횟수     | 시간\n")
                        .append("-------  | ------------------------\n");

                for (int i = 0; i < expTrain.size(); i++) {
                    ExpTrain train = expTrain.get(i);
                    sb.append(String.format("%-7s | %-24s%n",
                            train.getNowCount() + "/" + train.getTotalCount(),
                            train.getExpDate()));
                }

                sb.append("```"); // 코드블럭 끝
                event.getChannel().sendMessage(sb.toString()).queue();
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
            case "!렙반감", "!레벨반감":
                try {
                    File file = util.getFile("lvl_decrease.jpg");
                    event.getChannel().sendMessage("렙반감")
                            .addFiles(FileUpload.fromData(file))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!포뻥", "!포스":
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
            case "!시아":
                try {
                    File file = util.getFile("sia_skill_combo.png");
                    event.getChannel().sendMessage("시아 별자리 스킬 조합")
                            .addFiles(FileUpload.fromData(file))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!묵현":
                try {
                    File file1 = util.getFile("mux1.png");
                    File file2 = util.getFile("mux2.png");
                    File file3 = util.getFile("mux3.png");
                    File file4 = util.getFile("mux4.png");
                    File file5 = util.getFile("mux5.png");
                    File file6 = util.getFile("mux6.png");
                    File file7 = util.getFile("mux7.png");
                    event.getChannel().sendMessage("묵현 스킬 매커니즘")
                            .addFiles(FileUpload.fromData(file1))
                            .addFiles(FileUpload.fromData(file2))
                            .addFiles(FileUpload.fromData(file3))
                            .addFiles(FileUpload.fromData(file4))
                            .addFiles(FileUpload.fromData(file5))
                            .addFiles(FileUpload.fromData(file6))
                            .addFiles(FileUpload.fromData(file7))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!칠흑":
                try {
                    int random = (int) (Math.random() * 99) + 1;
                    event.getChannel().sendMessage("오늘 칠흑 먹을 확률은 **" + random + "%** 입니다.")
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!기대값", "!기댓값":
                try {
                    File file1 = util.getFile("starforce.png");
                    event.getChannel().sendMessage("개편 후 스타포스 기대값")
                            .addFiles(FileUpload.fromData(file1))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!에스페라", "!에페":
                try {
                    File file1 = util.getFile("esfera1.png");
                    File file2 = util.getFile("esfera.png");
                    event.getChannel()
                            .sendMessage(">>> ## 에스페라 주간퀘 가이드\n" +
                                    "이미지 혹은 아래 링크의 가이드 중 편한 방법으로 진행\n" +
                                    "- [미니맵 활용 가이드](<https://archive.is/j0esZ>)")
                            .addFiles(FileUpload.fromData(file1))
                            .addFiles(FileUpload.fromData(file2))
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
                        "- [서버 상태 확인](<https://maplestatus.info/en/maplestory/global/>) \n" +
                        "- [GMS 캐릭터검색](<https://maplestory.gg/>) \n" +
                        "- [각종 기대값 계산](<https://brendonmay.github.io/>) \n" +
                        "- [사냥터 빌드짜는 사이트](<https://maplemaps.net/>) \n" +
                        "- [닌자성 도우미](<https://godchangsub.github.io/>) \n" +
                        "- [보스 결정석 계산 사이트](<https://zydico.github.io/Website/#/maplestory-helper/boss-crystals/>) \n" +
                        ""
                ).queue();
                break;
            case "!영끌", "!영끌도핑" :
                event.getChannel().sendMessage("" +
                        "## **극한의 영끌 도핑**\n" +
                        "\n" +
                        "•  **탕윤의 절대미각** : 공/마 +10 (20분)\n" +
                        "  └ *방법: 탕윤의 요리교실 요리5번 완료 후 소금 n만큼 넣기*\n" +
                        "\n" +
                        "•  **유가든 랜덤버프** : 공/마 +20 (30분)\n" +
                        "  └ *방법: 유가든 일퀘 NPC 'Suan Ming'에게 코인 1개 지불 (1일 1회)*\n" +
                        "\n" +
                        "•  **결혼 버프** : 공/마 +60 (하객 1명 기준)\n" +
                        "\n" +
                        "•  **쇼와타운 랜덤마블** : 올스탯/공/마 +30 (30분)\n" +
                        "  └ *Peerless Marble 획득 (1일 1회)*\n" +
                        "  └ *참고 :  [유튜브](<https://youtu.be/GbO_gu4QFNQ>)*\n" +
                        "\n" +
                        "**추천 사용 순서**\n" +
                        "```\n" +
                        "탕윤 ➔  유가든 ➔  결혼 ➔  마이홈 ➔  인기도 ➔  랜덤마블(버프 수락을 전구로 가능)\n" +
                        "```"
                ).queue();
                break;
            case "!공략" :
                event.getChannel().sendMessage("" +
                        ">>> # 공략 모음 \n" +
                        "- [퍼밀리어](<https://archive.is/I8tUg>) \n" +
                        "- [퍼밀리어 뱃지작 정리](<https://discord.com/channels/1230391079317147688/1442839475708367029>) \n" +
                        "- [퍼밀리어 뱃지작 몹 리스트](<https://discord.com/channels/1230391079317147688/1442844421476585593>) \n" +
                        "- [팬텀 포레스트 (종결표창)](<https://archive.is/vrggv>) \n" +
                        "- [스텔라 탐정단 (무적링/크뎀링)](<https://archive.is/VlzSp>) \n" +
                        "- [커머시 (160제 얼장/눈장)](<https://archive.is/RrTqT>) \n" +
                        "- [닌자성 (펫버프 달려있는 공짜펫)](<https://archive.is/sJSea>) \n" +
                        "- [애프터랜드 (스펙 토템)](<https://youtu.be/CZ2jB3h6xjc>) \n" +
                        "- [버섯신사 (에스크 선행퀘)](<https://youtu.be/Y_GkulrmB8Y>) \n" +
                        "- [에스크 (재획비/광물)](<https://archive.is/Q5luF>) \n" +
                        "- [노히메 (보조/140반지)](<https://youtu.be/zXKJYay-zSI>) \n" +
                        "- [골럭스 (종결 장신구)](<https://archive.is/hEUvR>) \n" +
                        "- [멀티펫 하는법](<https://archive.is/7FvgU>) \n" +
                        "- [유가든 길뚫 (토템 종결 파밍)](<https://archive.is/WJ9lN>) \n" +
                        "- [유가든 일퀘 (토템 종결 파밍)](<https://archive.is/qfoJz>) \n" +
                        "- [템세팅 빌드](<https://arca.live/b/globalmaplestory/121755229>) \n" +
                        ""
                ).queue();
                break;
            case "!실" :
                try {
                    File file = util.getFile("kaling_gauge.png");
                    File file2 = util.getFile("kaling_gauge2.png");
                    event.getChannel().sendMessage("카링 게이지 사진")
                            .addFiles(FileUpload.fromData(file))
                            .addFiles(FileUpload.fromData(file2))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!추옵" :
                try {
                    File file = util.getFile("flame_score.png");
                    //https://archive.is/yGH06
                    event.getChannel().sendMessage(
                                "[추옵 계산 사이트](<https://www.whackybeanz.com/calc/equips/setup>) \n" +
                                    "[추옵 계산 사이트 가이드](<https://archive.is/WNEKB>) \n" +
                                    "## 장비 렙제별 적정 추옵표 \n" +
                                    "Mid 단계까지는 강/영환불, 그 이후로는 검환불 추천")
                            .addFiles(FileUpload.fromData(file))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!반상" :
                try {
                    File file = util.getFile("ring_box.png");
                    event.getChannel().sendMessage("반지 상자 확률 정리표")
                            .addFiles(FileUpload.fromData(file))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case "!몬파" :
                try {
                    File file = util.getFile("monster_park.png");
                    event.getChannel().sendMessage("몬스터파크 경험치 표 \n" +
                                    "```" +
                                    "- 50% 기준 (보약) : 1.5배 \n" +
                                    "- 50% + 50% (보약 + 일요일) : 2배 \n" +
                                    "- 50% + 50% + 30% 기준 (보약 + 일요일 + 모래시계) : 2.3배 \n" +
                                    "- 50% + 50% + 250% (보약 + 일요일 + 썬데이) : 4.5배 \n" +
                                    "- 50% + 50% + 30% + 250% (보약 + 일요일 + 썬데이 + 모래시계) : 4.8배 \n" +
                                    "```")
                            .addFiles(FileUpload.fromData(file))
                            .queue();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            /*case "!테스트" :
                String message = "@everyone 오늘은 수로 / 플래그 정산일입니다.\n" +
                        "수플 참여기록 없을 시 노블스킬 사용 제한됩니다.";
                event.getChannel().sendMessage(message).queue();
                break;*/
        }
    }
}
