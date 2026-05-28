package com.discord.bot.maple.bots;

import com.discord.bot.maple.bots.exp.ExpTrain;
import com.discord.bot.maple.bots.exp.ExpTrainHolder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class MessageReceiveListener extends ListenerAdapter {

    private final Util util;
    private final ExpTrainHolder expTrainHolder;

    public MessageReceiveListener(Util util, ExpTrainHolder expTrainHolder) {
        this.util = util;
        this.expTrainHolder = expTrainHolder;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String msg = event.getMessage().getContentDisplay();
        if (!msg.contains("!")) return;

        System.out.printf("[%s] %#s: %s\n",
                event.getChannel(),
                event.getAuthor(),
                msg);

        switch (msg) {
            case "!명령어", "!도움", "!help" -> handleHelp(event);
            case "!ping" -> event.getChannel().sendMessage("Pong!").queue();
            case "!아케인" -> event.getChannel().sendMessage("```\nArcane Symbol/claim\n```").queue();
            case "!어센틱" -> event.getChannel().sendMessage("```\nSacred Symbol/claim\n```").queue();
            case "!조각" -> event.getChannel().sendMessage("```\nSol Erda Fragment\n```").queue();
            case "!무토" -> sendImage(event, "메이플_무토_레시피.png", "무토 레시피");
            case "!연뿌" -> handleExpTrain(event);
            case "!5퍼" -> sendImage(event, "5percent.jpeg", "보스 5퍼");
            case "!렙반감", "!레벨반감" -> sendImage(event, "lvl_decrease.jpg", "렙반감");
            case "!포뻥", "!포스" -> sendImages(event, List.of("arcane_bbung.png", "sacred_bbung.png"), "보스별 포스 및 포뻥");
            case "!메소반감" -> sendImage(event, "meso_decrease.png", "메소반감");
            case "!경험치반감" -> sendImage(event, "exp_decrease.png", "경험치반감");
            case "!도핑" -> sendImage(event, "doping.png", "GMS 도핑목록");
            case "!시아" -> sendImage(event, "sia_skill_combo.png", "시아 별자리 스킬 조합");
            case "!묵현" -> sendImages(event, List.of("mux1.png", "mux2.png", "mux3.png", "mux4.png", "mux5.png", "mux6.png", "mux7.png"), "묵현 스킬 매커니즘");
            case "!칠흑" -> event.getChannel().sendMessage("오늘 칠흑 먹을 확률은 **" + ((int)(Math.random() * 99) + 1) + "%** 입니다.").queue();
            case "!기대값", "!기댓값" -> sendImage(event, "starforce.png", "개편 후 스타포스 기대값");
            case "!에스페라", "!에페" -> handleEsfera(event);
            case "!헤영지" -> handleHavenGuide(event);
            case "!유틸" -> handleUtil(event);
            case "!영끌", "!영끌도핑" -> handleMaxDoping(event);
            case "!공략" -> handleGuide(event);
            case "!실" -> sendImages(event, List.of("kaling_gauge.png", "kaling_gauge2.png"), "카링 게이지 사진");
            case "!추옵" -> handleFlame(event);
            case "!반상" -> sendImage(event, "ring_box.png", "반지 상자 확률 정리표");
            case "!몬파" -> handleMonsterPark(event);
        }
    }

    // ───────── 파일 전송 헬퍼 ─────────

    private void sendImage(MessageReceivedEvent event, String filename, String title) {
        try {
            File file = util.getFile(filename);
            event.getChannel().sendMessage(title)
                    .addFiles(FileUpload.fromData(file))
                    .queue();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendImages(MessageReceivedEvent event, List<String> filenames, String title) {
        try {
            List<FileUpload> uploads = new ArrayList<>();
            for (String filename : filenames) {
                uploads.add(FileUpload.fromData(util.getFile(filename)));
            }
            event.getChannel().sendMessage(title)
                    .addFiles(uploads)
                    .queue();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ───────── 명령어 핸들러 ─────────

    private void handleHelp(MessageReceivedEvent event) {
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
                "/보스일정확정\n" +
                "/보스일정초기화\n" +
                "```").queue();
    }

    private void handleExpTrain(MessageReceivedEvent event) {
        List<ExpTrain> expTrain = expTrainHolder.getTrains();
        if (expTrain.isEmpty()) {
            event.getChannel().sendMessage("등록된 연뿌가 없습니다.").queue();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("```")
                .append("횟수     | 시간\n")
                .append("-------  | ------------------------\n");

        for (ExpTrain train : expTrain) {
            sb.append(String.format("%-7s | %-24s%n",
                    train.getNowCount() + "/" + train.getTotalCount(),
                    train.getExpDate()));
        }

        sb.append("```");
        event.getChannel().sendMessage(sb.toString()).queue();
    }

    private void handleEsfera(MessageReceivedEvent event) {
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
    }

    private void handleHavenGuide(MessageReceivedEvent event) {
        event.getChannel().sendMessage(
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
    }

    private void handleUtil(MessageReceivedEvent event) {
        event.getChannel().sendMessage(
                ">>> # 유틸 사이트 \n" +
                "- [서버 상태 확인](<https://maplestatus.info/en/maplestory/global/>) \n" +
                "- [GMS 캐릭터검색](<https://maplestory.gg/>) \n" +
                "- [각종 기대값 계산](<https://brendonmay.github.io/>) \n" +
                "- [사냥터 빌드짜는 사이트](<https://maplemaps.net/>) \n" +
                "- [닌자성 도우미](<https://godchangsub.github.io/>) \n" +
                "- [보스 결정석 계산 사이트](<https://zydico.github.io/Website/#/maplestory-helper/boss-crystals/>) \n"
        ).queue();
    }

    private void handleMaxDoping(MessageReceivedEvent event) {
        event.getChannel().sendMessage(
                "## **극한의 영끌 도핑**\n\n" +
                "•  **탕윤의 절대미각** : 공/마 +10 (20분)\n" +
                "  └ *방법: 탕윤의 요리교실 요리5번 완료 후 소금 n만큼 넣기*\n\n" +
                "•  **유가든 랜덤버프** : 공/마 +20 (30분)\n" +
                "  └ *방법: 유가든 일퀘 NPC 'Suan Ming'에게 코인 1개 지불 (1일 1회)*\n\n" +
                "•  **결혼 버프** : 공/마 +60 (하객 1명 기준)\n\n" +
                "•  **쇼와타운 랜덤마블** : 올스탯/공/마 +30 (30분)\n" +
                "  └ *Peerless Marble 획득 (1일 1회)*\n" +
                "  └ *참고 :  [유튜브](<https://youtu.be/GbO_gu4QFNQ>)*\n\n" +
                "**추천 사용 순서**\n" +
                "```\n" +
                "탕윤 ➔  유가든 ➔  결혼 ➔  마이홈 ➔  인기도 ➔  랜덤마블(버프 수락을 전구로 가능)\n" +
                "```"
        ).queue();
    }

    private void handleGuide(MessageReceivedEvent event) {
        event.getChannel().sendMessage(
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
                "- [모루 팁 - 무기편](<https://archive.is/bLVsj>) \n" +
                "- [모루 팁 - 방어구편](<https://archive.is/j7dxR>) \n" +
                "- [모루 획득처 정리](<https://archive.is/KNcXU>) \n"
        ).queue();
    }

    private void handleFlame(MessageReceivedEvent event) {
        try {
            File file = util.getFile("flame_score.png");
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
    }

    private void handleMonsterPark(MessageReceivedEvent event) {
        try {
            File file = util.getFile("monster_park.png");
            event.getChannel().sendMessage(
                    "몬스터파크 경험치 표 \n" +
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
    }
}
