Phattern {
	var synth;
	
	var <pProxies;

	var length;

	var <channel;

	var <>buffers;

	var <outProxy;

	var <specs;

	*initClass {
		StartUp.add {
			SynthDef.writeOnce(\phampler, {
				arg out = 0, buffer, sustain = 1, startPos = 0, rate = 1, 
				amp = 0.1, pan = 0, cutoff = 18000, rez = 1;

				var sig, env;
				
				// so tbufferha sounds cut one another off nicely... like groups in a 
				// normal sampler
				env = EnvGen.ar(Env.linen(0, sustain, 0.01), doneAction: 2);

				sig = PlayBuf.ar(
					1, buffer, 
					rate: BufRateScale.kr(buffer) * rate,
					startPos: startPos * BufFrames.kr(buffer),
					doneAction: 2
				);

				sig = RLPF.ar(sig, cutoff, rez);

				sig = sig * amp;

				sig = Pan2.ar(sig, pan);

				OffsetOut.ar(out, sig);
			})
		};
	}

	*new { |synth|
		^super.new.initPhattern(synth);
	}

	initPhattern { |syn|
		this.synth_(syn);

		pProxies = IdentityDictionary();
		specs = IdentityDictionary();

		outProxy = EventPatternProxy();

		// create a mixer channel for audio and fx to live on
		channel = PhexChannel();

		buffers = IdentityDictionary();

		// set patterns to route audio to correct bits of mixer
		this.updatePattern(\out, channel.bus); 
		this.updatePattern(\group, channel.groups.sources); 
	}

	play {
		this.outProxy.play;
	}

	stop {
		this.outProxy.stop;
	}

	fx {
		^channel.fx;
	}

	synth_ { |syn|
		synth = syn;
		this.changed(\synth);
	}

	updatePattern { |param pattern|
		// if "pattern" is a string convert it to a Phex
		if(pattern.isString) { 
			var spec;

			if(specs.includesKey(param)) {
				// if a spec has been set for this param on 
				// this Phattern use that
				spec = specs[param];
			} {
				// otherwise look in synth metadata
				if(SynthDescLib.global[synth].notNil) {
					var synthDesc = SynthDescLib.global[synth];
					if(synthDesc.metadata.notNil) {	
						if(synthDesc.metadata.specs.notNil) {
							if(synthDesc.metadata[specs].notNil) {
								spec = synthDesc.metadata[specs][param];
							};			
						};
					};
				};
			};

			// if nonoe of tha above found a spec look one up in spec library
			spec = (spec ?? param).asSpec;

			pattern = Phex(pattern, spec);
		};

		// update pattern proxies
		if(pProxies[param].isNil) {
			pProxies[param] = PatternProxy(pattern);
		} {
			pProxies[param].source = pattern;
		};

		// update ouputted proxy
		outProxy.source = Pbind(
			\instrument, synth, *pProxies.getPairs
		);
	}

	// if we don't understand the message it's a pattern being updated or 
	// recalled.
	doesNotUnderstand { |selector val| 
		// if this is a setter update the pattern
		if(selector.isSetter) {
			selector = selector.asGetter;

			// handle special buffer key when strings are passed in by mapping
			// to buffers dictionary
			if((selector == \buffer) && (val.isString)) {
				var seq = List();
				val.do { |c|
					if(buffers[c.asSymbol].notNil) {
						// if we have a buffer assigned to this character use it
						seq.add(buffers[c.asSymbol]);
					} {
						// otherwise insert a rest
						seq.add(\rest);
					};
				};

				seq.postln;

				val = Pseq(seq, inf);
			};

			this.updatePattern(selector, val);
		} {
			// if it's a getter return the pattern that's already set
			^pProxies[selector].source;
		};
	}
}

