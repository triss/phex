PhexChannel {
	// bus the audio whizz's down
	var <bus;
	
	// groups for organising sound sources/fx/amps/sends
	var <groups;

	// bus for tracking RMS - useful fro gui
	var rmsBus;

	// dictionarys containing fx and sends
	var <fx, <sends;

	// amplitude and panning, an out send and an rms tracker for our channel
	var <ampPan, <outSend, <rmsTracker;

	*initClass {
		StartUp.add {
			// handles amp and pan of a channel - out is both in and out!
			SynthDef.writeOnce(
				\phexAmpPan, { |out=0 amp=1 pan=0|
					ReplaceOut.ar(out, Pan2.ar(In.ar(out, 2), pan, amp));
				}, 
				rates: [\ir, 0.05, 0.05],
				metadata: (specs: (amp: \amp, pan: \pan))
			);

			// sends from one channel to another
			SynthDef.writeOnce(
				\phexSend, { |in out=0 amt=0|
					Out.ar(out, In.ar(in, 2) * amt);
				},
				rates: [\ir, \ir, 0.05],
				metadata: (specs: (amt: \amp))
			);

			// rms tracker
			SynthDef.writeOnce(
				\phexAmpTracker, { |in=0 out| 
					Out.kr(out, PeakFollower.kr(In.ar(in, 2)).mean);
				}
			);
		};
	}

	*new { |out=0 parentGroup|
		^super.new.init(out, parentGroup);
	}

	init { |out, parentGroup|
		// set parent group to default group if none specified
		parentGroup = parentGroup ?? { Server.default };
	  
		// set up groups
		groups = IdentityDictionary(know: true);
		groups.channel = Group.tail(parentGroup);    // whole channel
	
		groups.sources = Group.head(groups.channel); // sources for channel
		groups.fx = Group.after(groups.sources);     // fx 
		groups.amps = Group.after(groups.fx);        // amp/pan controls
		groups.sends = Group.after(groups.amps);     // sends to other channels
	
		// set up bus
		bus = Bus.audio(Server.default, 2);

		// set up volume for channel
		ampPan = Synth(\phexAmpPan, [\out, bus], groups.amps);

		rmsBus = Bus.control(Server.default, 1);

		rmsTracker = Synth(
			\phexAmpTracker, [\in, bus, \out, rmsBus], ampPan, \addAfter
		);

		// set up output for channel
		outSend = Synth(\phexSend, [
			\in, bus, \out, out, \amt, 1], groups.sends
		);

		// set up dictionary for fx storage
		fx = IdentityDictionary(know: true);

		// set up dictionary for send storage
		sends = IdentityDictionary(know: true);
	}

	setPan { |pan=0| 
		ampPan.set(\pan, pan);
		this.changed(\pan, pan);
	}

	setAmp { |amp=0| 
		ampPan.set(\amp, amp);
		this.changed(\amp, amp);
	}

	addSend { |name toBus|
		sends[name] = Synth(\phexSend, [\in, bus, \out, toBus], groups.sends);
		this.changed(\sendAdded, name);
	}

	setSendAmt { |name amt=0| 
		sends[name].set(\amt, amt);
		this.changed(\sendAmt, name, amt);
	}

	setFx { |name synthDef args target fadeTime=0.1|
		var addAction = \addBefore;
		
		// if an fx already exists fade it out
		if(fx.contains[name]) {
			fx[name].release(fadeTime);
		};

		// if target (location in fx chain) not specified then
		// add to tail of fx group
		if(target.isNil) {
			target = groups.fx; 
			addAction = \addToTail;
		};

		// so long as a fx def name is specified insert the fx
		if(synthDef.notNil) {
			fx[name] = Synth(synthDef, args, target, addAction);
		};

		this.changed(\fx, name, synthDef);
	}

	makeView { |parent, bounds|
		var view, levelIndicator, levelUpdater;
	 
		bounds = bounds ?? { 50@250 };

		view = View(parent, bounds);

		levelIndicator = LevelIndicator();

		view.layout_(VLayout(
			Knob()
				.centered_(true)
				.value_(0.5)
				.action_({ |k|
					this.setPan(\pan.asSpec.map(k.value));
				}),
			HLayout(
				[
					Slider()
						.value_(\db.asSpec.unmap(1.ampdb))
						.action_({ |sl|
							this.setAmp(\db.asSpec.map(sl.value).dbamp);
						}),
					stretch: 3
				],
				[levelIndicator,stretch: 1],
			),
			*sends.collect { |v, k| 
				VLayout(
					StaticText().string_(k).align_(\center),
					Knob()
						.action_({ |k|
							this.setSendAmt(\amp.asSpec.map(k.value));
						});
				)
			}.asArray.flatten
		));

		levelUpdater = {
			inf.do {
				rmsBus.get({ |amp| { levelIndicator.value = amp.postln }.defer });
				0.1.wait;
			};
		}.fork;

		view.onClose = {
			levelUpdater.stop;
		};

		^view
	}
}
