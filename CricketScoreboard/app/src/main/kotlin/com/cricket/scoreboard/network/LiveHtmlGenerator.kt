package com.cricket.scoreboard.network

import com.cricket.scoreboard.model.Match

object LiveHtmlGenerator {

    fun generateHtml(match: Match?, matchCode: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>🏏 Live Cricket Scoreboard - $matchCode</title>
  <style>
    :root {
      --bg-dark: #0a1b12;
      --card-bg: #112d1e;
      --card-border: #1d4a32;
      --turf-green: #257d42;
      --turf-green-light: #34a853;
      --pitch-gold: #f5c518;
      --wicket-red: #ea4335;
      --text-main: #ffffff;
      --text-muted: #a3c2b1;
      --subtle-bg: #0d2317;
    }
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      -webkit-tap-highlight-color: transparent;
    }
    body {
      background-color: var(--bg-dark);
      color: var(--text-main);
      padding: 12px;
      display: flex;
      justify-content: center;
    }
    .container {
      width: 100%;
      max-width: 580px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .header-bar {
      background: linear-gradient(135deg, #0d381e, #18522c);
      padding: 12px 16px;
      border-radius: 14px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      box-shadow: 0 4px 14px rgba(0,0,0,0.3);
      border: 1px solid var(--card-border);
    }
    .live-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: rgba(234, 67, 53, 0.2);
      color: var(--wicket-red);
      font-weight: 800;
      font-size: 11px;
      letter-spacing: 0.5px;
      padding: 4px 8px;
      border-radius: 20px;
      border: 1px solid rgba(234, 67, 53, 0.4);
    }
    .live-dot {
      width: 7px;
      height: 7px;
      background-color: var(--wicket-red);
      border-radius: 50%;
      animation: pulse 1.4s infinite;
    }
    @keyframes pulse {
      0% { transform: scale(0.95); opacity: 0.8; }
      50% { transform: scale(1.3); opacity: 1; }
      100% { transform: scale(0.95); opacity: 0.8; }
    }
    .match-code-tag {
      background: rgba(245, 197, 24, 0.15);
      color: var(--pitch-gold);
      font-weight: bold;
      font-size: 11px;
      padding: 4px 8px;
      border-radius: 6px;
      border: 1px solid rgba(245, 197, 24, 0.3);
    }
    .card {
      background-color: var(--card-bg);
      border-radius: 16px;
      padding: 14px 16px;
      border: 1px solid var(--card-border);
      box-shadow: 0 4px 16px rgba(0,0,0,0.25);
    }
    .match-title {
      font-size: 16px;
      font-weight: 800;
      color: var(--text-main);
      margin-bottom: 2px;
    }
    .toss-info {
      font-size: 11px;
      color: var(--text-muted);
    }
    .score-hero {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      margin-top: 10px;
      padding-top: 10px;
      border-top: 1px solid rgba(255,255,255,0.08);
    }
    .score-big {
      font-size: 46px;
      font-weight: 900;
      line-height: 1;
      color: #ffffff;
      display: flex;
      align-items: baseline;
    }
    .wickets-part {
      color: var(--pitch-gold);
      font-size: 32px;
      margin-left: 2px;
    }
    .overs-part {
      text-align: right;
    }
    .overs-text {
      font-size: 16px;
      font-weight: 700;
      color: #ffffff;
    }
    .crr-text {
      font-size: 12px;
      color: var(--text-muted);
      margin-top: 2px;
    }
    .target-banner {
      background: rgba(0, 0, 0, 0.35);
      border-radius: 10px;
      padding: 8px 12px;
      margin-top: 10px;
      color: var(--pitch-gold);
      font-size: 12px;
      font-weight: 600;
      display: flex;
      justify-content: space-between;
      border: 1px solid rgba(245, 197, 24, 0.2);
    }
    .result-banner {
      background: linear-gradient(90deg, #d4a017, #f5c518);
      color: #000;
      border-radius: 12px;
      padding: 12px;
      text-align: center;
      font-weight: 800;
      font-size: 14px;
      margin-bottom: 4px;
    }
    .over-strip {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-top: 8px;
      overflow-x: auto;
      padding-bottom: 2px;
    }
    .ball-pill {
      min-width: 28px;
      height: 28px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 11px;
      font-weight: 800;
      color: #ffffff;
      background: #2a5a3c;
    }
    .ball-pill.wicket { background: var(--wicket-red); }
    .ball-pill.four { background: var(--turf-green-light); color: #000; font-weight: 900; }
    .ball-pill.six { background: var(--pitch-gold); color: #000; font-weight: 900; }
    .ball-pill.extra { background: #e67e22; font-size: 10px; padding: 0 4px; border-radius: 12px; }
    .ball-pill.dot { background: rgba(255,255,255,0.12); color: var(--text-muted); }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 12.5px;
    }
    th {
      text-align: left;
      font-size: 10.5px;
      color: var(--text-muted);
      font-weight: 600;
      padding-bottom: 6px;
      border-bottom: 1px solid rgba(255,255,255,0.08);
    }
    th.num, td.num {
      text-align: right;
    }
    td {
      padding: 7px 0;
      border-bottom: 1px solid rgba(255,255,255,0.04);
    }
    tr:last-child td { border-bottom: none; }
    .striker-star {
      color: var(--pitch-gold);
      font-weight: 900;
    }
    .section-title {
      font-size: 12px;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--pitch-gold);
      margin-bottom: 8px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .tab-btn-row {
      display: flex;
      gap: 6px;
      margin-bottom: 10px;
    }
    .tab-btn {
      flex: 1;
      padding: 6px 10px;
      border-radius: 8px;
      background: rgba(255,255,255,0.08);
      border: 1px solid rgba(255,255,255,0.1);
      color: var(--text-muted);
      font-weight: bold;
      font-size: 11px;
      cursor: pointer;
    }
    .tab-btn.active {
      background: var(--turf-green);
      color: #fff;
      border-color: var(--turf-green-light);
    }
    .status-toast {
      font-size: 10px;
      color: var(--text-muted);
      text-align: center;
      margin-top: 4px;
    }
    .flash {
      animation: highlight 0.8s ease;
    }
    @keyframes highlight {
      0% { background-color: rgba(245, 197, 24, 0.35); }
      100% { background-color: transparent; }
    }
  </style>
</head>
<body>
  <div class="container">
    <!-- Top Header -->
    <div class="header-bar">
      <div style="display: flex; align-items: center; gap: 8px;">
        <span style="font-size: 18px;">🏏</span>
        <div>
          <div style="font-weight: 900; font-size: 13px; letter-spacing: 0.5px;">CRICKET LIVE</div>
          <div style="font-size: 10px; color: var(--text-muted);">Real-Time Scoreboard</div>
        </div>
      </div>
      <div style="display: flex; align-items: center; gap: 6px;">
        <div class="live-badge"><span class="live-dot"></span> LIVE</div>
        <div class="match-code-tag">$matchCode</div>
      </div>
    </div>

    <!-- Match Result Banner (if finished) -->
    <div id="resultBanner" style="display: none;" class="result-banner"></div>

    <!-- Main Score Card -->
    <div class="card" id="mainScoreCard">
      <div class="match-title" id="matchTitle">Match Starting Soon...</div>
      <div class="toss-info" id="tossInfo">Waiting for toss and opening players</div>

      <div class="score-hero">
        <div>
          <div class="score-big">
            <span id="totalRuns">0</span>
            <span class="wickets-part">/<span id="wickets">0</span></span>
          </div>
          <div style="font-size: 11px; color: var(--pitch-gold); font-weight: 600; margin-top: 2px;" id="battingTeamLabel"></div>
        </div>
        <div class="overs-part">
          <div class="overs-text"><span id="oversText">0.0</span> / <span id="totalOvers">20</span> Ov</div>
          <div class="crr-text">CRR: <span id="crr">0.00</span></div>
        </div>
      </div>

      <div id="targetBanner" class="target-banner" style="display: none;">
        <span>🎯 Target: <b id="targetScore">-</b></span>
        <span id="targetEquation">-</span>
      </div>

      <!-- Current Over Ball-by-ball -->
      <div style="margin-top: 12px; padding-top: 8px; border-top: 1px solid rgba(255,255,255,0.06);">
        <div style="font-size: 11px; color: var(--text-muted); font-weight: 600;">This Over:</div>
        <div class="over-strip" id="overStrip">
          <span style="font-size: 11px; color: var(--text-muted); font-style: italic;">No balls bowled yet</span>
        </div>
      </div>
    </div>

    <!-- Active Batsmen Card -->
    <div class="card">
      <div class="section-title">🏏 Batsmen</div>
      <table>
        <thead>
          <tr>
            <th>Batter</th>
            <th class="num">R</th>
            <th class="num">B</th>
            <th class="num">4s</th>
            <th class="num">6s</th>
            <th class="num">SR</th>
          </tr>
        </thead>
        <tbody id="batsmenBody">
          <tr><td colspan="6" style="text-align: center; color: var(--text-muted);">Waiting for batsmen...</td></tr>
        </tbody>
      </table>
    </div>

    <!-- Current Bowler Card -->
    <div class="card">
      <div class="section-title">⚾ Current Bowler</div>
      <table>
        <thead>
          <tr>
            <th>Bowler</th>
            <th class="num">O</th>
            <th class="num">M</th>
            <th class="num">R</th>
            <th class="num">W</th>
            <th class="num">Econ</th>
          </tr>
        </thead>
        <tbody id="bowlerBody">
          <tr><td colspan="6" style="text-align: center; color: var(--text-muted);">Waiting for bowler...</td></tr>
        </tbody>
      </table>
    </div>

    <!-- Full Scorecard Accordion / Section -->
    <div class="card">
      <div class="section-title">
        <span>📋 Full Scorecard</span>
        <button id="toggleScorecardBtn" onclick="toggleScorecard()" style="background: none; border: none; color: var(--pitch-gold); font-size: 11px; font-weight: bold; cursor: pointer;">Show Full Card ▼</button>
      </div>

      <div id="scorecardContent" style="display: none; margin-top: 8px;">
        <div class="tab-btn-row" id="innTabRow">
          <button class="tab-btn active" onclick="switchScorecardTab(1)" id="tabInn1">1st Innings</button>
          <button class="tab-btn" onclick="switchScorecardTab(2)" id="tabInn2">2nd Innings</button>
        </div>

        <div style="font-weight: 700; font-size: 12px; color: var(--pitch-gold); margin: 6px 0;" id="scorecardInnHeader">Batting</div>
        <table>
          <thead>
            <tr>
              <th>Batter</th>
              <th class="num">R</th>
              <th class="num">B</th>
              <th class="num">4s</th>
              <th class="num">6s</th>
              <th class="num">SR</th>
            </tr>
          </thead>
          <tbody id="fullBattingBody"></tbody>
        </table>

        <div style="font-size: 11px; color: var(--text-muted); margin: 8px 0; padding: 6px; background: rgba(0,0,0,0.25); border-radius: 6px;" id="extrasInfo"></div>

        <div style="font-weight: 700; font-size: 12px; color: var(--pitch-gold); margin: 12px 0 6px 0;">Bowling</div>
        <table>
          <thead>
            <tr>
              <th>Bowler</th>
              <th class="num">O</th>
              <th class="num">M</th>
              <th class="num">R</th>
              <th class="num">W</th>
              <th class="num">Econ</th>
            </tr>
          </thead>
          <tbody id="fullBowlingBody"></tbody>
        </table>
      </div>
    </div>

    <div class="status-toast" id="syncStatus">🟢 Connected • Auto-updating live ball-by-ball</div>
  </div>

  <script>
    let currentMatchData = null;
    let scorecardVisible = false;
    let activeScorecardTab = 1;
    let lastDeliveryCount = -1;

    function toggleScorecard() {
      scorecardVisible = !scorecardVisible;
      document.getElementById('scorecardContent').style.display = scorecardVisible ? 'block' : 'none';
      document.getElementById('toggleScorecardBtn').innerText = scorecardVisible ? 'Hide Full Card ▲' : 'Show Full Card ▼';
    }

    function switchScorecardTab(tabNum) {
      activeScorecardTab = tabNum;
      document.getElementById('tabInn1').className = tabNum === 1 ? 'tab-btn active' : 'tab-btn';
      document.getElementById('tabInn2').className = tabNum === 2 ? 'tab-btn active' : 'tab-btn';
      renderScorecard();
    }

    function renderScorecard() {
      if (!currentMatchData) return;
      const inn = activeScorecardTab === 1 ? currentMatchData.innings1 : (currentMatchData.innings2 || currentMatchData.innings1);
      if (!inn) return;

      document.getElementById('scorecardInnHeader').innerText = (inn.battingTeamName || 'Batting') + ' - ' + inn.totalRuns + '/' + inn.wickets + ' (' + (inn.oversText || (Math.floor(inn.legalBalls/6) + '.' + (inn.legalBalls%6))) + ' Ov)';

      // Batting
      const batBody = document.getElementById('fullBattingBody');
      batBody.innerHTML = '';
      (inn.batsmen || []).forEach(b => {
        if (!b.isBatting && !b.isOut && (b.balls || 0) === 0) return;
        const tr = document.createElement('tr');
        const sr = (b.balls > 0) ? ((b.runs / b.balls) * 100).toFixed(1) : '0.0';
        tr.innerHTML = '<td><b>' + b.name + (b.isBatting ? '*' : '') + '</b><div style="font-size:10px; color:var(--text-muted);">' + (b.dismissalText || (b.isBatting ? 'not out' : '')) + '</div></td>' +
                       '<td class="num"><b>' + b.runs + '</b></td>' +
                       '<td class="num">' + b.balls + '</td>' +
                       '<td class="num">' + b.fours + '</td>' +
                       '<td class="num">' + b.sixes + '</td>' +
                       '<td class="num">' + sr + '</td>';
        batBody.appendChild(tr);
      });

      // Extras
      const extras = document.getElementById('extrasInfo');
      extras.innerText = 'Extras: ' + (inn.totalExtras || (inn.wides + inn.noBalls + inn.legByes + inn.byes)) +
                         ' (w ' + (inn.wides||0) + ', nb ' + (inn.noBalls||0) + ', lb ' + (inn.legByes||0) + ', b ' + (inn.byes||0) + ')';

      // Bowling
      const bowlBody = document.getElementById('fullBowlingBody');
      bowlBody.innerHTML = '';
      (inn.bowlers || []).forEach(bw => {
        if ((bw.legalBalls || 0) === 0 && (bw.runsConceded || 0) === 0) return;
        const tr = document.createElement('tr');
        const overs = Math.floor(bw.legalBalls / 6) + '.' + (bw.legalBalls % 6);
        const econ = (bw.legalBalls > 0) ? ((bw.runsConceded * 6.0) / bw.legalBalls).toFixed(2) : '0.00';
        tr.innerHTML = '<td><b>' + bw.name + '</b></td>' +
                       '<td class="num">' + (bw.oversText || overs) + '</td>' +
                       '<td class="num">' + (bw.maidens || 0) + '</td>' +
                       '<td class="num">' + (bw.runsConceded || 0) + '</td>' +
                       '<td class="num" style="color:var(--wicket-red); font-weight:bold;">' + (bw.wickets || 0) + '</td>' +
                       '<td class="num">' + econ + '</td>';
        bowlBody.appendChild(tr);
      });
    }

    function updateUI(match) {
      if (!match) return;
      currentMatchData = match;

      document.getElementById('matchTitle').innerText = match.teamA.name + ' vs ' + match.teamB.name;
      const toss = match.tossResult;
      const tossWinner = (toss && toss.winnerTeamId === match.teamA.id) ? match.teamA.name : match.teamB.name;
      const tossChoice = (toss && toss.choice === 'BAT') ? 'Bat' : 'Bowl';
      document.getElementById('tossInfo').innerText = tossWinner + ' won toss & elected to ' + tossChoice;

      const inn = (match.currentInningsIndex === 2 && match.innings2) ? match.innings2 : match.innings1;
      if (!inn) return;

      document.getElementById('battingTeamLabel').innerText = (inn.battingTeamName || 'Batting Team').toUpperCase();
      document.getElementById('totalRuns').innerText = inn.totalRuns || 0;
      document.getElementById('wickets').innerText = inn.wickets || 0;

      const oversText = inn.oversText || (Math.floor(inn.legalBalls / 6) + '.' + (inn.legalBalls % 6));
      document.getElementById('oversText').innerText = oversText;
      document.getElementById('totalOvers').innerText = match.totalOvers || 20;

      const crr = (inn.legalBalls > 0) ? ((inn.totalRuns * 6.0) / inn.legalBalls).toFixed(2) : '0.00';
      document.getElementById('crr').innerText = crr;

      // Result
      const resBanner = document.getElementById('resultBanner');
      if (match.isFinished && match.resultMessage) {
        resBanner.style.display = 'block';
        resBanner.innerText = '🏆 ' + match.resultMessage;
      } else {
        resBanner.style.display = 'none';
      }

      // Target Banner for 2nd innings
      const tgtBanner = document.getElementById('targetBanner');
      if (match.currentInningsIndex === 2 && inn.targetScore) {
        tgtBanner.style.display = 'flex';
        document.getElementById('targetScore').innerText = inn.targetScore;
        const reqRuns = Math.max(0, inn.targetScore - inn.totalRuns);
        const remBalls = Math.max(0, (match.totalOvers * 6) - inn.legalBalls);
        document.getElementById('targetEquation').innerText = 'Need ' + reqRuns + ' from ' + remBalls + ' balls';
      } else {
        tgtBanner.style.display = 'none';
      }

      // Over Strip
      const overStrip = document.getElementById('overStrip');
      const deliveries = inn.deliveries || [];
      const currentOverIndex = Math.floor(inn.legalBalls / 6);
      const currentOverDeliveries = deliveries.filter(d => d.overIndex === currentOverIndex);

      if (currentOverDeliveries.length === 0) {
        overStrip.innerHTML = '<span style="font-size: 11px; color: var(--text-muted); font-style: italic;">Starting new over...</span>';
      } else {
        overStrip.innerHTML = '';
        currentOverDeliveries.forEach(d => {
          const pill = document.createElement('div');
          if (d.isWicket) {
            pill.className = 'ball-pill wicket';
            pill.innerText = 'W';
          } else if (d.extraType === 'WIDE') {
            pill.className = 'ball-pill extra';
            pill.innerText = (d.extraRuns > 1 ? d.extraRuns : '') + 'wd';
          } else if (d.extraType === 'NO_BALL') {
            pill.className = 'ball-pill extra';
            pill.innerText = (d.runsBat > 0 ? d.runsBat : '') + 'nb';
          } else if (d.runsBat === 4) {
            pill.className = 'ball-pill four';
            pill.innerText = '4';
          } else if (d.runsBat === 6) {
            pill.className = 'ball-pill six';
            pill.innerText = '6';
          } else if (d.runsBat === 0) {
            pill.className = 'ball-pill dot';
            pill.innerText = '•';
          } else {
            pill.className = 'ball-pill';
            pill.innerText = d.runsBat;
          }
          overStrip.appendChild(pill);
        });
      }

      // Active Batsmen
      const striker = (inn.batsmen || []).find(b => b.playerId === match.currentStrikerId);
      const nonStriker = (inn.batsmen || []).find(b => b.playerId === match.currentNonStrikerId);
      const batBody = document.getElementById('batsmenBody');
      batBody.innerHTML = '';

      if (striker) {
        const tr = document.createElement('tr');
        const sr = (striker.balls > 0) ? ((striker.runs / striker.balls) * 100).toFixed(1) : '0.0';
        tr.innerHTML = '<td><b>' + striker.name + '</b> <span class="striker-star">*</span></td>' +
                       '<td class="num"><b>' + striker.runs + '</b></td>' +
                       '<td class="num">' + striker.balls + '</td>' +
                       '<td class="num">' + striker.fours + '</td>' +
                       '<td class="num">' + striker.sixes + '</td>' +
                       '<td class="num">' + sr + '</td>';
        batBody.appendChild(tr);
      }
      if (nonStriker) {
        const tr = document.createElement('tr');
        const sr = (nonStriker.balls > 0) ? ((nonStriker.runs / nonStriker.balls) * 100).toFixed(1) : '0.0';
        tr.innerHTML = '<td>' + nonStriker.name + '</td>' +
                       '<td class="num"><b>' + nonStriker.runs + '</b></td>' +
                       '<td class="num">' + nonStriker.balls + '</td>' +
                       '<td class="num">' + nonStriker.fours + '</td>' +
                       '<td class="num">' + nonStriker.sixes + '</td>' +
                       '<td class="num">' + sr + '</td>';
        batBody.appendChild(tr);
      }

      // Active Bowler
      const bowler = (inn.bowlers || []).find(bw => bw.playerId === match.currentBowlerId);
      const bowlBody = document.getElementById('bowlerBody');
      bowlBody.innerHTML = '';
      if (bowler) {
        const tr = document.createElement('tr');
        const overs = Math.floor(bowler.legalBalls / 6) + '.' + (bowler.legalBalls % 6);
        const econ = (bowler.legalBalls > 0) ? ((bowler.runsConceded * 6.0) / bowler.legalBalls).toFixed(2) : '0.00';
        tr.innerHTML = '<td><b>' + bowler.name + '</b></td>' +
                       '<td class="num">' + (bowler.oversText || overs) + '</td>' +
                       '<td class="num">' + (bowler.maidens || 0) + '</td>' +
                       '<td class="num">' + (bowler.runsConceded || 0) + '</td>' +
                       '<td class="num" style="color:var(--wicket-red); font-weight:bold;">' + (bowler.wickets || 0) + '</td>' +
                       '<td class="num">' + econ + '</td>';
        bowlBody.appendChild(tr);
      }

      // Flash on new delivery
      if (lastDeliveryCount !== -1 && deliveries.length > lastDeliveryCount) {
        const card = document.getElementById('mainScoreCard');
        card.classList.remove('flash');
        void card.offsetWidth; // trigger reflow
        card.classList.add('flash');
      }
      lastDeliveryCount = deliveries.length;

      // Update full scorecard if open
      if (scorecardVisible) {
        renderScorecard();
      }
    }

    // Polling loop every 1.5 seconds
    async function pollMatch() {
      try {
        const resp = await fetch('/api/match');
        if (resp.ok) {
          const data = await resp.json();
          updateUI(data);
          document.getElementById('syncStatus').innerText = '🟢 Connected • Auto-updating live (' + new Date().toLocaleTimeString() + ')';
        }
      } catch (e) {
        document.getElementById('syncStatus').innerText = '🟡 Reconnecting to Scorer...';
      }
    }

    pollMatch();
    setInterval(pollMatch, 1500);
  </script>
</body>
</html>
        """.trimIndent()
    }
}
