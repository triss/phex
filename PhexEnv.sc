PhexEnv {
	*new {
		var phexEnv;

		// storage of phatterns and parameters
		var phatterns;
		var params;
	 
		var tempoClock, group;
		var setBpm, makeMixerWindow;

		// set up MIDI connectivity
		MIDIClient.init;
		MIDIIn.connectAll;

		// set up a load of useful specs
		PhexSpecs.add;

		// our environment
		phexEnv = EnvironmentRedirect();

		// keep track of all the phatterns here
		phatterns = IdentityDictionary();

		// and all the parameter specs
		params = IdentityDictionary();

		// our tempo clock
		tempoClock = TempoClock.default;

		// allocate a dummy buffer for use as silence with phamplers...
		// very hacky but it works
		Buffer.alloc(Server.default, 1000);

		// updates our bpm
		setBpm = { |bpm|
			tempoClock.tempo = bpm / 60;
			phexEnv.changed(\bpm, bpm);

			// also update all synths beatDur??
		};

		// display mixer window
		makeMixerWindow = {
			var window, view, constructView, controller;

			window = Window("Phex", Rect(0, 0, 800, 200));

			// populate the view
			constructView = {
				var channelViews;

				// if we already have a populated mixer view destroy it
				if(view.notNil) { view.remove };

				view = View(window, Rect(0, 0, 800, 200));

				// pull out mixer channel view from each Phattern
				channelViews = phatterns.collect { |phattern, key| 
					VLayout(
						StaticText().string_(key).align_(\center),
						phattern.channel.makeView
					);
				}.asArray;

				// arrange channel views in layout
				view.layout = HLayout(*channelViews);
			};

			constructView.();
	
			// update gui every time phattern is added to environment
			controller = SimpleController(phexEnv);
			controller.put(\phatternAdded, { constructView.() });

			window.front;
		};	

		makeMixerWindow.();

		// these helper functions are exposed to the user
		phexEnv.make({
			// make an fx def that'll work with Phex mixer channels
			~addFxDef = { |name func specs lags lib|
				SynthDef(name, {
					var env, sig = In.ar(\out.ir, 2);
			
					env = Linen.kr(\gate.kr(1), 0.01, 1, 0.01, 2);

					sig = SynthDef.wrap(func, lags, [sig]);

					XOut.ar(\out.ir, \dryWet.kr(1) * env, sig);
				}, metadata: (specs: specs)).add(lib);
			};

			// make a percussive synth def that'll work with Phatterns/Pbind etc
			~addPercDef = { |name func specs lags lib|
				SynthDef(name, {
					var sig, env;
					
					env = EnvGen.ar(
						Env.perc(0.01, \sustain.ir(1)), 
						doneAction: 2
					);

					sig = SynthDef.wrap(func, lags, [env]);

					sig = sig * env * \amp.ir(0.1) * AmpComp.kr(\freq.ir(440));

					sig = Splay.ar(sig, center: \pan.ir(0));

					OffsetOut.ar(\out.ir, sig);
				}, metadata: (specs: specs)).add(lib);
			};

			// make a gated adsr synth def that'll work with Phatterns/Pbind etc
			~addAdsrDef = { |name func specs lags lib|
				SynthDef(name, {
					var sig, env;
					
					env = EnvGen.ar(
						Env.adsr(
							\att.kr(0.1), 
							\dec.kr(0.3), 
							\sus.kr(0.5), 
							\rel.kr(1)
						), 
						\gate.kr(1),
						doneAction: 2
					);

					sig = SynthDef.wrap(func, lags, [env]);

					sig = sig * env * \amp.kr(0.1) * AmpComp.kr(\freq.kr(440));

					sig = Splay.ar(sig, center: \pan.ir(0));

					OffsetOut.ar(\out.ir, sig);
				}, metadata: (specs: specs)).add(lib);
			};

			// this allows paramaters of patterns to be controlled via MIDI
			// upon execution it waits for an incoming MIDI value
			~midiMap = { |spec|
				var midiFunc, cc, chan, src, value = 128;
			
				spec = spec.asSpec;
			
				// create a midi responder that listens to eveything 
				midiFunc = MIDIFunc.cc({ |val ccNum chanNum srcID|
					// remember the cc etc.
					cc = ccNum; chan = chanNum; src = srcID;
				
					// free this midiFunc
					midiFunc.free;
				
					// create a new MIDI responder that only listens to 
					// that controller
					midiFunc = MIDIFunc.cc({ |val|
						value = val;
					}, cc, chan, src);
				});
			
				// create a Pfunc that outputs the value we just got
				Pfunc({ spec.map(value / 128) });
			};

			// allow access for debugging
			~phatterns = {
				phatterns;
			};

			~params = {
				params;
			};
		});

		// .dispatch lets us check up on what's been bunged in the environment
		// it's called every time anything is stored in to environment variable
		phexEnv.dispatch = { |key val| 
			case(
				// bpm 
				{ key == \bpm }, { setBpm.(val) },

				// if it's a buffer just store it - I often need these hanging around
				{ val.isKindOf(Buffer) }, { },
				{ val.isKindOf(Function) }, { },

				// sometimes i want collections of things hanging around to store stuff in
				{ (val.isKindOf(Collection)) && (val.isKindOf(String).not) }, { },

				// if it's a phattern
				{ val.isKindOf(Phattern) }, {
					// keep track of it
					phatterns.put(key, val);

					// tell the phattern about params we have stored away
					params.keysValuesDo { |param value|
						val.updatePattern(param, value);
					};

					// update UI
					phexEnv.changed(\phatternAdded);
				},

				// if it's none of the above it's a shared parameter
				{ 
					// tell phatterns about the new parameter
					phatterns.do { |phattern|
						phattern.updatePattern(key, val);
					};

					// store it away so we can tell 
					// other phatterns about it later

					params[key] = val;
				};
			);
		};
	
		^phexEnv;
	}
}
