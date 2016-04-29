package com.emotiv.mentalcommand;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.emotiv.getdata.EngineConnector;
import com.emotiv.getdata.EngineInterface;
import com.emotiv.insight.MentalCommandDetection.IEE_MentalCommandTrainingControl_t;
import com.emotiv.insight.IEmoStateDLL.IEE_MentalCommandAction_t;
import com.emotiv.insight.MentalCommandDetection;
import com.emotiv.spinner.AdapterSpinner;
import com.emotiv.spinner.DataSpinner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;


public class Training extends Activity implements EngineInterface {
	
	EngineConnector engineConnector;
	
	 Spinner spinAction;
	 Button btnTrain,btnClear; 
	 ProgressBar progressBarTime,progressPower;
	 AdapterSpinner spinAdapter;
	 ImageView imgBox;
	 ArrayList<DataSpinner> model = new ArrayList<DataSpinner>();
	 int indexAction, _currentAction,userId=0,count=0;
	Handler timehandler = new Handler();
	 
	 Timer timer;
	 TimerTask timerTask;
	 
	 float _currentPower = 0;
	 float startLeft     = -1;
	 float startRight    = 0;
	 float widthScreen   = 0;

	 int CommandCount = 0;
	  
	 boolean isTrainning = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.training_activity);
		engineConnector = EngineConnector.shareInstance();
		engineConnector.delegate = this;
		init();
	}
	public void init()
	{
			spinAction=(Spinner)findViewById(R.id.spinnerAction);
			btnTrain=(Button)findViewById(R.id.btstartTraing);
			btnClear=(Button)findViewById(R.id.btClearData);
			btnClear.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					switch (indexAction) {
					case 0:
						engineConnector.trainningClear(IEE_MentalCommandAction_t.MC_NEUTRAL.ToInt());
						break;
					case 1:
						engineConnector.trainningClear(IEE_MentalCommandAction_t.MC_PUSH.ToInt());
						break;
					case 2:
						engineConnector.trainningClear(IEE_MentalCommandAction_t.MC_PULL.ToInt());
						break;
					case 3:
						engineConnector.trainningClear(IEE_MentalCommandAction_t.MC_LEFT.ToInt());
						break;
					case 4:
						engineConnector.trainningClear(IEE_MentalCommandAction_t.MC_RIGHT.ToInt());
						break;
					default:
						break;
					}
				}
			});
			progressBarTime=(ProgressBar)findViewById(R.id.progressBarTime);
			progressBarTime.setVisibility(View.INVISIBLE);
			progressPower=(ProgressBar)findViewById(R.id.ProgressBarpower);

			
			setDataSpinner();
			spinAction.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					indexAction=arg2;
				}
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
				}
			});
			btnTrain.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(!engineConnector.isConnected)
						Toast.makeText(Training.this,"You need to connect to your headset.",Toast.LENGTH_SHORT).show();
					else{
						switch (indexAction) {
							case 0:
								startTrainingMentalcommand(IEE_MentalCommandAction_t.MC_NEUTRAL);
								break;
							case 1:
								engineConnector.enableMentalcommandActions(IEE_MentalCommandAction_t.MC_PUSH);
								startTrainingMentalcommand(IEE_MentalCommandAction_t.MC_PUSH);
								break;
							case 2:
								engineConnector.enableMentalcommandActions(IEE_MentalCommandAction_t.MC_PULL);
								startTrainingMentalcommand(IEE_MentalCommandAction_t.MC_PULL);
								break;
						case 3:
							engineConnector.enableMentalcommandActions(IEE_MentalCommandAction_t.MC_LEFT);
							startTrainingMentalcommand(IEE_MentalCommandAction_t.MC_LEFT);
							break;
						case 4:
							engineConnector.enableMentalcommandActions(IEE_MentalCommandAction_t.MC_RIGHT);
							startTrainingMentalcommand(IEE_MentalCommandAction_t.MC_RIGHT);
							break;
							default:
								break;
						}
					}
				}
			});

			Timer timerListenAction = new Timer();
			timerListenAction.scheduleAtFixedRate(new TimerTask() {
			    @Override
			    public void run() {
			    	handlerUpdateUI.sendEmptyMessage(1);
			    }
			},
			0, 20);	
			
	}
	Handler handlerUpdateUI=new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				Log.d("HandlerCase0","Made it into the case 0 of the HandlerUpdateUI");
				count ++;
				int trainningTime=(int)MentalCommandDetection.IEE_MentalCommandGetTrainingTime(userId)[1]/1000;
				if(trainningTime > 0)
					progressBarTime.setProgress(count / trainningTime);
				if (progressBarTime.getProgress() >= 100) {
					timerTask.cancel();
					timer.cancel();
				}
				break;
			case 1:
				CommandCount++;
				if ((CommandCount % 80) == 0) {
					Log.d("HandlerCaseDefault", "Made it into the case 1 of the HandlerUpdateUI");
					fulfillCommand();
				}
				break;
			default:
				Log.d("HandlerCaseDefault", "Made it into the Default case of the HandlerUpdateUI");
				break;
			}
		};
	};

	public void startTrainingMentalcommand(IEE_MentalCommandAction_t MentalCommandAction) {
		isTrainning = engineConnector.startTrainingMetalcommand(isTrainning, MentalCommandAction);
		btnTrain.setText((isTrainning) ? "Abort Training" : "Train");
	}
	
	public void setDataSpinner()
	{
		model.clear();
		DataSpinner data = new DataSpinner();
		data.setTvName("Home");
		data.setChecked(engineConnector.checkTrained(IEE_MentalCommandAction_t.MC_NEUTRAL.ToInt()));
		model.add(data);
		/* Neutral should be set to direct to home screen*/
		data=new DataSpinner();
		data.setTvName("Dial");
		data.setChecked(engineConnector.checkTrained(IEE_MentalCommandAction_t.MC_PUSH.ToInt()));
		model.add(data);
		/* Dial is set to make phone calls*/
		data=new DataSpinner();
		data.setTvName("Maps");
		data.setChecked(engineConnector.checkTrained(IEE_MentalCommandAction_t.MC_PULL.ToInt()));
		model.add(data);
		/* Maps is set to pull up Google Map*/
		data=new DataSpinner();
		data.setTvName("Music");
		data.setChecked(engineConnector.checkTrained(IEE_MentalCommandAction_t.MC_LEFT.ToInt()));
		model.add(data);
		/* Music is set to open Spotify or any music application*/
		/*data=new DataSpinner();
		data.setTvName("Email");
		data.setChecked(engineConnector.checkTrained(IEE_MentalCommandAction_t.MC_RIGHT.ToInt()));
		model.add(data);
		/*Email is meant to open Gmail
		data=new DataSpinner();
		data.setTvName("Camera");
		data.setChecked(engineConnector.checkTrained(IEE_MentalCommandAction_t.MC_LIFT.ToInt()));
		model.add(data);*/


		spinAdapter = new AdapterSpinner(this, R.layout.row, model);
		spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinAction.setAdapter(spinAdapter);
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_trainning, menu);
		return true;
	}
	public void TimerTask()
	{
		count = 0;
		timerTask=new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				handlerUpdateUI.sendEmptyMessage(0);
			}
		};
	}
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		    Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			widthScreen = size.x;
	}
	
	private void fulfillCommand() {
		float power = _currentPower;
		if(isTrainning){
				//Figure out what to do when isTraining is true....
			onPause();
		}
		if(( _currentAction == IEE_MentalCommandAction_t.MC_PULL.ToInt() && power > 0)) {

			Log.d("MapsCode", "Code is executing in maps code");
			//Code for Google Maps
			// Create a Uri from an intent string. Use the result to create an Intent.
			Uri gmmIntentUri = Uri.parse("google.streetview:cbll=46.414382,10.013988");
			// Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
			Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
			// Make the Intent explicit by setting the Google Maps package
			mapIntent.setPackage("com.google.android.apps.maps");
			// Attempt to start an activity that can handle the Intent
			startActivity(mapIntent);


		}
		if(((_currentAction == IEE_MentalCommandAction_t.MC_PUSH.ToInt())  && power > 0)) {
			//Code for Phone app
			Intent intent = new Intent(Intent.ACTION_DIAL);
			intent.setData(Uri.parse("tel:2145524890"));
			startActivity(intent);

		}
		if(((_currentAction == IEE_MentalCommandAction_t.MC_LEFT.ToInt()) && power > 0)) {
			Intent intent = new Intent();
			intent.setAction(android.content.Intent.ACTION_VIEW);
			File file = new File("/sdcard/Ringtones/06_Let_Me_Love_You__feat.m4a");
			intent.setDataAndType(Uri.fromFile(file), "audio/*");
			startActivity(intent);
		}
		/*if(((_currentAction == IEE_MentalCommandAction_t.MC_RIGHT.ToInt()) && power > 0)) {
			Intent mailClient = new Intent(Intent.ACTION_VIEW);
			mailClient.setClassName("com.google.android.gm", "com.google.android.gm.ConversationListActivity");
			startActivity(mailClient);
		}
		if(((_currentAction == IEE_MentalCommandAction_t.MC_LIFT.ToInt()) && power > 0)) {
			/*PUT CODE TO OPEN CAMERA APP HERE
		}*/
 		}
	public void enableClick()
	{
		btnClear.setClickable(true);
		spinAction.setClickable(true);
	}
	@Override
	public void userAdd(int userId) {
		// TODO Auto-generated method stub
		this.userId=userId;
	}
	@Override
	public void currentAction(int typeAction, float power) {
		// TODO Auto-generated method stub
		progressPower.setProgress((int)(power*100));
		_currentAction = typeAction;
		_currentPower  = power;
	}

	@Override
	public void userRemoved() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void trainStarted() {
		// TODO Auto-generated method stub
		progressBarTime.setVisibility(View.VISIBLE);
		btnClear.setClickable(false);
		spinAction.setClickable(false);
		 timer = new Timer();
		 TimerTask();
		 timer.schedule(timerTask, 0, 10);
	}

	@Override
	public void trainSucceed() {
		// TODO Auto-generated method stub
		progressBarTime.setVisibility(View.INVISIBLE);
		btnTrain.setText("Train");
		enableClick();
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				Training.this);
		// set title
		alertDialogBuilder.setTitle("Training Succeeded");
		// set dialog message
		alertDialogBuilder
				.setMessage("Training is successful. Accept this training?")
				.setCancelable(false)
				.setIcon(R.drawable.ic_launcher)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(
									DialogInterface dialog,int which) {
								engineConnector.setTrainControl(IEE_MentalCommandTrainingControl_t.MC_ACCEPT.getType());
							}
						})
				.setNegativeButton("No",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								engineConnector.setTrainControl(IEE_MentalCommandTrainingControl_t.MC_REJECT.getType());
							}
						});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	@Override
	public  void trainFailed(){
		progressBarTime.setVisibility(View.INVISIBLE);
		btnTrain.setText("Train");
		enableClick();
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				Training.this);
		// set title
		alertDialogBuilder.setTitle("Training Failed");
		// set dialog message
		alertDialogBuilder
				.setMessage("Signal is noisy. Can't training")
				.setCancelable(false)
				.setIcon(R.drawable.ic_launcher)
				.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(
									DialogInterface dialog, int which) {

							}
						});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
		isTrainning = false;
	}

	@Override
	public void trainCompleted() {
		// TODO Auto-generated method stub
		DataSpinner data=model.get(indexAction);
		data.setChecked(true);
		model.set(indexAction, data);
		spinAdapter.notifyDataSetChanged();
		isTrainning = false;
	}

	@Override
	public void trainRejected() {
		// TODO Auto-generated method stub
		DataSpinner data=model.get(indexAction);
		data.setChecked(false);
		model.set(indexAction, data);
		spinAdapter.notifyDataSetChanged();
		enableClick();
		isTrainning = false;
	}

	@Override
	public void trainErased() {
		// TODO Auto-generated method stub
		 new AlertDialog.Builder(this)
	    .setTitle("Training Erased")
	    .setMessage("")
	    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	     public void onClick(DialogInterface dialog, int which) { 
	        }
	     })
	    .setIcon(android.R.drawable.ic_dialog_alert)
	     .show();
		DataSpinner data=model.get(indexAction);
		data.setChecked(false);
		model.set(indexAction, data);
		spinAdapter.notifyDataSetChanged();
		enableClick();
		isTrainning = false;
	}
	
	@Override
	public void trainReset() {
		// TODO Auto-generated method stub
		if(timer!=null){
			timer.cancel();
			timerTask.cancel();
		}
		isTrainning = false;
		progressBarTime.setVisibility(View.INVISIBLE);
		progressBarTime.setProgress(0);
		enableClick();
	};
	
	public void onBackPressed() {
		 android.os.Process.killProcess(android.os.Process.myPid());
		  finish(); 
	 }
}
