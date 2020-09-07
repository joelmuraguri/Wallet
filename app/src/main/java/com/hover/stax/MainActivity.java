package com.hover.stax;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hover.sdk.api.Hover;
import com.hover.sdk.api.HoverParameters;
import com.hover.stax.actions.Action;
import com.hover.stax.channels.ChannelsActivity;
import com.hover.stax.channels.UpdateChannelsWorker;
import com.hover.stax.database.KeyStoreExecutor;
import com.hover.stax.home.BalanceModel;
import com.hover.stax.home.HomeViewModel;
import com.hover.stax.onboard.SplashScreenActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	final public static String CHECK_ALL_BALANCES = "CHECK_ALL";

	private HomeViewModel homeViewModel;
	private static List<Action> toRun;
	private static List<String> hasRun;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		BottomNavigationView navView = findViewById(R.id.nav_view);
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
				R.id.navigation_home, R.id.navigation_buyAirtime, R.id.navigation_moveMoney, R.id.navigation_security)
														  .build();
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
		NavigationUI.setupWithNavController(navView, navController);

		homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

		shouldRun(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		shouldRun(intent);
	}

	private void shouldRun(Intent intent) {
		if (intent.getAction() != null && intent.getAction().equals(CHECK_ALL_BALANCES))
			startRun();
	}

	public void startRun() {
		hasRun = new ArrayList<>();
		runAllBalances();
	}

	private void runAllBalances() {
		homeViewModel.getBalanceActions().observe(this, actions -> {
			toRun = new ArrayList<>(actions.size());
			toRun.addAll(actions);
			chooseRun(0);
		});
	}

	private void chooseRun(int index) {
		if (toRun != null && toRun.size() > hasRun.size()) {
			makeHoverCall(toRun.get(index), index);
		}
	}

	private void makeHoverCall(Action action, int runId) {
		HoverParameters.Builder builder = new HoverParameters.Builder(this);
		builder.request(action.public_id);
//			builder.setEnvironment(HoverParameters.PROD_ENV);
		builder.style(R.style.myHoverTheme);
		builder.finalMsgDisplayTime(2000);
		builder.extra("pin", KeyStoreExecutor.decrypt(homeViewModel.getChannel(action.channel_id).pin, ApplicationInstance.getContext()));
		Intent i = builder.buildIntent();
		startActivityForResult(i, runId);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (hasRun == null) { hasRun = new ArrayList<>(); }
		hasRun.add(data.getStringExtra("action_id"));
		chooseRun(requestCode + 1);
	}
}


