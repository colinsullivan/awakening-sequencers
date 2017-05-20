/**
 *  @file       abletonlink_tempoclock.sc
 *
 *	@desc       This is an example for syncing a TempoClock to Ableton Link.
 *
 *  @author     Colin Sullivan <colin [at] colin-sullivan.net>
 *
 *  @copyright  2017 Colin Sullivan
 *  @license    Licensed under the MIT license.
 **/

(
  var store, lastBeatFloor, pat, patPlayer, clockOffsetSeconds, patPlayed = false, sequencers, clockController;

  API.mountDuplexOSC();

  s.boot();

  // I needed this to acutally sync with ableton
  clockOffsetSeconds = 0;

  store = StateStore.getInstance();
  sequencers = IdentityDictionary.new();
  clockController = ReduxAbletonTempoClockController.new((store: store, clockOffsetSeconds: clockOffsetSeconds));

  // define a simple synth
  SynthDef(\simple, {
    arg freq, amp = 0.7;
    var out;
    out = SinOsc.ar(freq, 0, amp) * EnvGen.kr(Env.linen(0.001, 0.05, 0.3), doneAction: 2);
    Out.ar(0, [out, out]);
  }).add();

  pat = Pbind(
    // the name of the SynthDef to use for each note
    \instrument, \simple,
    // MIDI note numbers -- converted automatically to Hz
    //\midinote, Pseq([60, 72, 71, 67, 69, 71, 72, 60, 69, 67], inf),
    \midinote, Pseq([96, 84, 84, 84], inf),
    // rhythmic values
    //\dur, Pseq([2, 2, 1, 0.5, 0.5, 1, 1, 2, 2, 4], inf)
    \dur, Pseq([1], inf)
  );



  // when state changes, this method will be called
  //lastBeatFloor = 0;
  store.subscribe({
    var state = store.getState();


    if ((state.sequencers != nil) && (state.sequencers.metro != nil) && (sequencers['metro'] == nil), {
      sequencers['metro'] = MetronomeSequencer.new((store: store, sequencerId: 'metro'));
    });


    //var beat = state.abletonlink.beat;
    //var bpm = state.abletonlink.bpm;
    //var tempo;
    //var secondsPerBeat;
    //var beatFloor = beat.floor();


    // simple example, skipps the Sequencer abstraction layer
    //if (lastBeatFloor != beatFloor, {
      //"beatFloor:".postln;
      //beatFloor.postln;
      ////"clockController.isReady():".postln;
      ////clockController.isReady().postln;
      ////"clockController.clock:".postln;
      ////clockController.clock.postln;
      //if (clockController.isReady().and(patPlayed == false), {
        //"playing...".postln();
        //patPlayed = true;
        //patPlayer = ReduxEventStreamPlayer.new(
          //store,
          //"metro",
          //stream: pat.asStream()
        //);
        //patPlayer.play(clockController.clock, quant: [0]);

      //});

      ////if (beatFloor % 3 == 0, {
        ////noteFreq = 880;
      ////}, {
        ////noteFreq = 440;
      ////});
      ////s.makeBundle(secondsPerBeat, {Synth(\simple, [freq: noteFreq, amp: 0.4]); });

      //lastBeatFloor = beatFloor;    
    //});



  });
)
