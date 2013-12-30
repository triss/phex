PhexChannel {
	var bus;
	
	var groups;
	
	var <fx, <sends;

	var <ampPan, <outSend;

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
				\phexSend, { |in=0 out=0 amt=0|
					Out.ar(out, In.ar(0, 2) * amt);
				},
				rates: [\ir, \ir, 0.05],
				metadata: (specs: (amt: \amt))
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

		// set up output for channel
		outSend = Synth(\phexSend, [
			\in, bus, \out, out, \amt, 1], groups.sends
		);

		// set up dictionary for fx storage
		fx = IdentityDictionary(know: true);
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

	addSend { |name toBus|
		sends[name] = Synth(\phexSend, [\in, bus, \out, toBus], groups.sends);
		this.changed(\sendAdded, name);
	}
}
