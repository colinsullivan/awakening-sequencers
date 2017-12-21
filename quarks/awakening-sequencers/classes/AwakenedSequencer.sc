
/**
 *  @class        AwakenedSequencer
 *
 *  @classdesc    A framework for playing a stream in sync with a clock.
 *  Subclasses handle setting up the stream and patch that the stream is
 *  playing.
 **/
AwakenedSequencer : Object {

  // reference to our state store
  var store,
    // the key for this sequencer (as a symbol) in the sequencers list in the
    // state tree
    sequencerId,
    // last known state
    <>currentState,
    // if we are sending our sequence output to a SuperCollider synth, do so
    // through an instance of the cruciallib's [Patch](https://github.com/crucialfelix/crucial-library)
    // abstraction
    // patch could be false if just MIDI out
    patch,
    stream,
    // a patch needs an audio output channel
    patchOutputChannel,
    // the MIDI output for this sequencer
    midiOut,
    // Currently, built to be an AbletonTempoClockController
    clockController,
    // number representing SC audio output channel
    outputBus,
    streamPlayer,
    // if this sequencer uses buffers this is a reference to the
    // `BufferManager` instance
    bufManager,
    clock = false;

  *new {
    arg params;

    ^super.new.init(params);
  }

  getStateSlice {
    //var stateAddressComponents,
      //stateSlice = store.getState();
    //// get slice of state tree
    //stateAddressComponents = sequencerId.split($.);
    //while({
      //(stateAddressComponents.size() > 0);
    //}, {
      //stateSlice = stateSlice[stateAddressComponents.removeAt(0).asSymbol()];
    //});
    //^stateSlice;
    var state = store.getState();
    ^state.sequencers[sequencerId.asSymbol()];
  }

  isReady {
    ^currentState.isReady;
  }

  init {
    arg params;

    store = params['store'];
    sequencerId = params['sequencerId'];

    bufManager = params['bufManager'];
    
    if (params['outputBus'] == nil, {
      outputBus = 0;
    }, {
      outputBus = params['outputBus'];
    });

    currentState = this.getStateSlice();
    if (params['clockController'] != nil, {
      clockController = params['clockController']    
    }, {
      //"Creating new ReduxAbletonTempoClockController".postln();
      clockController = ReduxAbletonTempoClockController.new((
        store: store,
        clockOffsetSeconds: currentState.clockOffsetSeconds
      ));
    });

    patchOutputChannel = this.create_output_channel();
    midiOut = this.initMidiOut();
    patch = this.initPatch();
    stream = this.initStream();

    // watch state store for updates
    this.handleStateChange();
    store.subscribe({
      this.handleStateChange();
    });
  }
  
  create_output_channel {
    arg parentOutputChannel;
    ^MixerChannel.new(
      "AwakenedSequencer[" ++ currentState.sequencerId ++ "]" ,
      Server.default,
      2, 2,
      outbus: outputBus
    );
  }

  initMidiOut {
    if (currentState.midiOutDeviceName != false, {
      ^MIDIOut.newByName(
        currentState.midiOutDeviceName,
        currentState.midiOutPortName
      ).latency_(Server.default.latency);
    });
  }

  handleStateChange {
    var state = store.getState(),
      lastState = currentState;
    currentState = this.getStateSlice();

    //"AwakenedSequencer.handleStateChange".postln();

    if (clock == false, {
      //"initializing clock...".postln();

      if (clockController.isReady(), {
        clock = clockController.clock;
        //"dispatching ready..".postln();
        store.dispatch((
          type: "AWAKENING-SEQUENCERS-SEQ_READY",
          payload: (
            sequencerId: sequencerId
          )
        ));
      }, {
        ^this;
      });

    });

    // if readyness changes
    if (currentState.isReady != lastState.isReady, {
      lastState.isReady = currentState.isReady;
      // go into playingState
      switch(currentState.playingState)
        {"QUEUED"} {
          this.queue();
        }
        {"PLAYING"} {
          this.play();
        }
        {"STOP_QUEUED"} {
          this.queueStop();
        }
    });


    // if playing state has changed
    if (currentState.playingState != lastState.playingState, {
      //"AwakenedSequencer::playingState changed".postln();
      //"lastState.playingState:".postln;
      //lastState.playingState.postln;
      lastState.playingState = currentState.playingState;
      //"currentState.playingState:".postln;
      //currentState.playingState.postln;
      switch(currentState.playingState)
        {"QUEUED"} {
          this.queue();
        }
        {"PLAYING"} {
          this.play();
        }
        {"STOP_QUEUED"} {
          this.queueStop();
        }
        {"STOPPED"} {
          this.stop();
        }
    });

  }

  initStream {
    // subclasses implement this method to create a pattern generator
    // and return its stream
  }

  initPatch {
    // subclasses implement this method to create a sound generator driven
    // by the stream
  }

  queue {
    //"AwakenedSequencer.queue".postln();
    if (streamPlayer != nil, {
      streamPlayer.stop();    
    });

    /**
     *  The ReduxEventStreamPlayer instance will dispatch next beat
     *  events to the Redux state store as they happen.
     *
     *  https://github.com/colinsullivan/supercollider-redux
     */
    streamPlayer = ReduxEventStreamPlayer.new(
      store,
      sequencerId,
      // this AwakenedSequencer's stream
      stream: stream
    );

    patchOutputChannel.play(
      streamPlayer,
      (
        clock: clock,
        quant: currentState.playQuant
      )
    );

    clock.play({
      //"AwakenedSequencer: queued clock playing...".postln();
      // if we're still queued
      if (currentState.playingState == "QUEUED", {
        // inform state store we've started playing
        //"Dispatching...".postln();
        store.dispatch((
          type: "AWAKENING-SEQUENCERS-SEQ_PLAYING",
          payload: (
            sequencerId: sequencerId
          )
        ));
      });
    }, currentState.playQuant);
  }

  play {

  }

  queueStop {
    clock.play({
      var theStreamPlayer = streamPlayer;
      //"Dispatching...".postln();
      theStreamPlayer.stop();
      store.dispatch((
        type: "AWAKENING-SEQUENCERS-SEQ_STOPPED",
        payload: (
          sequencerId: sequencerId
        )
      ));
    }, currentState.stopQuant);
  }

  stop {
    //"AwakenedSequencer.stop".postln();
    // stop immediately
    if (streamPlayer != nil, {
      streamPlayer.stop();    
    });
    stream = this.initStream();
  }
}
