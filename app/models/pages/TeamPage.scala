package models.pages

import java.time.LocalDate

import cats.implicits._
import com.fijimf.deepfij.schedule.model.{ConferenceStandings, Game, Result, Schedule, ScheduleRoot, Season, Team, WonLossRecord}
import play.api.Logging

case class TeamPage
(
  t:Team,
  s:Season,
  d:LocalDate,
  games: List[GameLine],
  conferenceStandings:ConferenceStandings,
  overallRecord:WonLossRecord,
  conferenceRecord:WonLossRecord
) {
  def name: String = t.name
  def conferenceName: String = conferenceStandings.conference.shortName
  def nickname: String = t.nickname
  def logo: String = t.logoUrl
  def logoDkBg: String = logo
  def logoLtBg: String = logo.replace("/bgd/","/bgl/")
  def season: Int = s.year
}

object TeamPage extends Logging {
  def create(root: ScheduleRoot, teamKey: String): Option[TeamPage] = {
    for {
      sched: Schedule <-root.schedules.find(sched=>root.currentSeason().contains(sched.season))
      t: Team <- sched.teamByKey.get(teamKey)
      s = sched.season
      d = LocalDate.now()
      gs = sched.gamesForTeamWithResults(t)
      z<-sched.conferenceMapping.find(cm=>cm.seasonId==s.id && cm.teamId==t.id)
      c<-sched.conferenceById.get(z.conferenceId)

    } yield {
      logger.info(s"teamKey=>$teamKey: ${t.id}, ${s.id}, ${gs.size}, ${c.id}")
      val standings: ConferenceStandings = sched.conferenceStandings(c)
      val conferenceGames: List[(Game, Option[Result])] = gs.filter(tup => sched.isConferenceGame(tup._1))
      val games: List[GameLine] = gs.map(tup => GameLine.create(sched.teamById, t, tup._1, tup._2))
      TeamPage(t, s, d, games, standings, WonLossRecord.from(t, gs), WonLossRecord.from(t, conferenceGames))
    }
  }
}

case class GameLine(date: LocalDate, isAt: Boolean, oppName: String, oppKey: String, isWin: Option[Boolean], score: Option[Int], oppScore: Option[Int])

object GameLine {
  def create(teamMap: Map[Long, Team], t: Team, g: Game, o: Option[Result]): GameLine = {
    if (t.id === g.awayTeamId) {
      GameLine(
        g.date,
        true,
        teamMap.get(g.homeTeamId).map(_.name).getOrElse(""),
        teamMap.get(g.homeTeamId).map(_.key).getOrElse(""),
        o.map(r => r.awayScore > r.homeScore),
        o.map(_.awayScore),
        o.map(_.homeScore)
      )
    } else {
      GameLine(
        g.date,
        false,
        teamMap.get(g.awayTeamId).map(_.name).getOrElse(""),
        teamMap.get(g.awayTeamId).map(_.key).getOrElse(""),
        o.map(r => r.homeScore > r.awayScore),
        o.map(_.homeScore),
        o.map(_.awayScore)
      )
    }
  }
}