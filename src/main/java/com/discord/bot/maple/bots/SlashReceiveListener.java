package com.discord.bot.maple.bots;

import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SlashReceiveListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();
        Util util = Util.getInstance();

        switch(command) {
            case "ping":
                event.reply("**Pong!**").queue();
                break;
            case "조각계산":
                CoreCalculator coreCalculator = CoreCalculator.getInstance();
                String coreType = event.getOption("코어타입").getAsString();
                int currentLevel = event.getOption("현재레벨").getAsInt();
                int targetLevel = event.getOption("목표레벨").getAsInt();

                // 유효성 검사
                if (currentLevel < 0 || currentLevel >= targetLevel || targetLevel > 30) {
                    event.reply("현재 레벨은 0 이상, 목표 레벨은 30이하여야 하며 목표 레벨은 현재 레벨보다 높아야 합니다.").queue();
                    break;
                }
                int piece = coreCalculator.calculatePiece(coreType, currentLevel, targetLevel);

                File file = util.getFile("erda_piece.png");

                event.reply("**" + coreType + "** 의 **" + currentLevel + "레벨** 부터 **" + targetLevel + "레벨** 까지\n" +
                                "필요한 조각 개수는 **" + piece + " 개** 입니다.")
                        .addFiles(FileUpload.fromData(file)) // 이미지 파일 추가
                        .queue();
                /*event.getChannel()
                        .sendFiles(FileUpload.fromData(file)) // 이미지를 먼저 업로드
                        .queue(message -> {
                            // 이미지 업로드가 완료된 후 텍스트 메시지 전송
                            message.getChannel()
                                    .sendMessage("**" + coreType + "** 의 **" + currentLevel + "레벨** 부터 **" + targetLevel + "레벨** 까지" + "\n" +
                                            "필요한 조각 개수는 **" + piece + " 개** 입니다.")
                                    .queue();
                        });*/
                break;
        }
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandDatas = new ArrayList<>();
        commandDatas.add(
                Commands.slash("ping", "Pong을 해줍니다.")
        );
        commandDatas.add(
                Commands.slash("조각계산", "필요한 조각개수를 계산합니다.")
        );

        event.getGuild().updateCommands().addCommands(commandDatas).queue();
    }
}
