package cn.qcofa.com;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.UUID;

public class HomeFragment extends Fragment {

    private EditText usernameInput;
    private EditText customUuidInput;
    private Spinner userTypeSpinner;
    private TextView uuidDisplay;
    private EditText ramValueInput;
    private SwitchMaterial legalCheck, devModsCheck, customRamCheck, demoModeCheck;
    private Button manualInstallJreBtn;
    private Button viewAccountsBtn;
    private Button saveVersionListBtn;
    private Button skinChangeBtn;
    private Button installQuestCraftBtn;
    private Button diagnoseQuestCraftBtn;
    private Spinner themeStyleSpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化UI组件
        initViews(view);
        
        // 加载并显示当前账号信息
        loadAndShowCurrentAccount();
        
        // 设置按钮点击事件
        setupClickListeners(view);
    }

    private void initViews(View view) {
        usernameInput = view.findViewById(R.id.usernameInput);
        customUuidInput = view.findViewById(R.id.customUuidInput);
        userTypeSpinner = view.findViewById(R.id.userTypeSpinner);
        uuidDisplay = view.findViewById(R.id.uuidDisplay);
        ramValueInput = view.findViewById(R.id.ramValueInput);
        
        legalCheck = view.findViewById(R.id.legalCheck);
        devModsCheck = view.findViewById(R.id.devModsCheck);
        customRamCheck = view.findViewById(R.id.customRamCheck);
        demoModeCheck = view.findViewById(R.id.demoModeCheck);
        manualInstallJreBtn = view.findViewById(R.id.manualInstallJreBtn);
        viewAccountsBtn = view.findViewById(R.id.viewAccountsBtn);
        saveVersionListBtn = view.findViewById(R.id.saveVersionListBtn);
        skinChangeBtn = view.findViewById(R.id.skinChangeBtn);
        installQuestCraftBtn = view.findViewById(R.id.installQuestCraftBtn);
        diagnoseQuestCraftBtn = view.findViewById(R.id.diagnoseQuestCraftBtn);
        themeStyleSpinner = view.findViewById(R.id.themeStyleSpinner);

        // 设置用户类型选择器
        setupUserTypeSpinner();

        // 设置界面风格选择器
        setupThemeStyleSpinner();

        // 设置默认值
        ramValueInput.setText("2048");
    }

    private void setupUserTypeSpinner() {
        // 创建适配器并添加选项
        android.widget.ArrayAdapter<CharSequence> adapter = android.widget.ArrayAdapter.createFromResource(
                requireContext(), 
                R.array.user_types_array, 
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userTypeSpinner.setAdapter(adapter);
        
        // 设置默认选中项为"msa"
        userTypeSpinner.setSelection(0);
    }

    private void setupThemeStyleSpinner() {
        android.widget.ArrayAdapter<CharSequence> adapter = android.widget.ArrayAdapter.createFromResource(
                requireContext(),
                R.array.theme_styles_array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeStyleSpinner.setAdapter(adapter);

        // 读取保存的主题风格并设置选中项
        String savedStyle = requireContext().getSharedPreferences("theme_style", android.content.Context.MODE_PRIVATE)
                .getString("style", "Material");
        int selection = savedStyle.equals("Miuix") ? 1 : 0;
        themeStyleSpinner.setSelection(selection);

        // 监听选择变化，切换主题
        themeStyleSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedStyle = parent.getItemAtPosition(position).toString();
                String currentStyle = requireContext().getSharedPreferences("theme_style", android.content.Context.MODE_PRIVATE)
                        .getString("style", "Material");
                if (!selectedStyle.equals(currentStyle)) {
                    // 保存选择并重启Activity
                    requireContext().getSharedPreferences("theme_style", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putString("style", selectedStyle)
                            .apply();
                    requireActivity().recreate();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners(View view) {
        Button createAccountBtn = view.findViewById(R.id.createAccountBtn);
        createAccountBtn.setOnClickListener(v -> createAccountFile());

        Button saveConfigBtn = view.findViewById(R.id.saveConfigBtn);
        saveConfigBtn.setOnClickListener(v -> saveConfigFiles());

        manualInstallJreBtn.setOnClickListener(v -> showJreInstallationDialog());
        
        viewAccountsBtn.setOnClickListener(v -> showAccountsList());

        saveVersionListBtn.setOnClickListener(v -> saveVersionListToStorage());

        skinChangeBtn.setOnClickListener(v -> showSkinChangeDialog());

        installQuestCraftBtn.setOnClickListener(v -> installToQuestCraft());
        diagnoseQuestCraftBtn.setOnClickListener(v -> showQuestCraftDiagnostics());

        // 设置折叠/展开功能的点击事件
        LinearLayout expandableSectionHeader = view.findViewById(R.id.expandableSectionHeader);
        TextView expandIndicator = view.findViewById(R.id.expandIndicator);
        LinearLayout expandableSection = view.findViewById(R.id.expandableSection);
        
        expandableSectionHeader.setOnClickListener(v -> {
            boolean isExpanded = expandableSection.getVisibility() == View.VISIBLE;
            if (isExpanded) {
                expandableSection.setVisibility(View.GONE);
                expandIndicator.setText("▶");
            } else {
                expandableSection.setVisibility(View.VISIBLE);
                expandIndicator.setText("▼");
            }
        });
        
        // 为用户名输入框添加文本变化监听器，自动触发UUID生成
        usernameInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                autoGenerateUUIDIfNeeded();
            }
        });
        
        // 为自定义UUID输入框添加文本变化监听器
        customUuidInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                // 当用户输入自定义UUID时，更新显示
                String customUuid = customUuidInput.getText().toString().trim();
                if (!customUuid.isEmpty()) {
                    uuidDisplay.setText("UUID: " + customUuid);
                } else {
                    // 如果清空了自定义UUID，则重新自动生成
                    autoGenerateUUIDIfNeeded();
                }
            }
        });
    }

    private void installToQuestCraft() {
        String uuid = extractUUIDFromDisplay();
        if (uuid == null || uuid.trim().isEmpty()) {
            Toast.makeText(requireContext(), "请先创建或选择账号", Toast.LENGTH_SHORT).show();
            return;
        }

        File storageDir = getStorageDir();
        File accountFile = new File(storageDir, uuid + ".json");
        File launcherConf = new File(storageDir, "launcher.conf");

        if (!accountFile.isFile() || !launcherConf.isFile()) {
            Toast.makeText(requireContext(), "请先创建账号文件并保存配置", Toast.LENGTH_LONG).show();
            return;
        }

        installQuestCraftBtn.setEnabled(false);
        Toast.makeText(requireContext(), "正在准备 QuestCraft 安装文件...", Toast.LENGTH_SHORT).show();

        Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            QuestCraftInstaller.Result result =
                    QuestCraftInstaller.install(appContext, accountFile, launcherConf);

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                installQuestCraftBtn.setEnabled(true);
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(result.success ? "QuestCraft 安装完成" : "QuestCraft 安装未完成")
                        .setMessage(result.message)
                        .setPositiveButton("确定", null)
                        .show();
            });
        }, "questcraft-installer").start();
    }

    private void showQuestCraftDiagnostics() {
        diagnoseQuestCraftBtn.setEnabled(false);
        Context appContext = requireContext().getApplicationContext();

        new Thread(() -> {
            String report = QuestCraftInstaller.diagnose(appContext);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                diagnoseQuestCraftBtn.setEnabled(true);
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("QuestCraft 诊断")
                        .setMessage(report)
                        .setPositiveButton("确定", null)
                        .show();
            });
        }, "questcraft-diagnostics").start();
    }

    private void generateUUID() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            // 如果用户名为空，生成随机用户名
            username = "Player_" + System.currentTimeMillis() % 10000;
            usernameInput.setText(username);
        }

        // 生成离线UUID - 使用与shell脚本相同的算法
        String offline = "offline player:" + username;
        String md5 = md5Hash(offline);
        
        // 格式化为UUID格式
        if (md5.length() >= 32) {
            String formattedUUID = String.format("%s-%s-%s-%s-%s",
                md5.substring(0, 8),
                md5.substring(8, 12),
                md5.substring(12, 16),
                md5.substring(16, 20),
                md5.substring(20, 32));
            
            uuidDisplay.setText("UUID: " + formattedUUID);
            Toast.makeText(requireContext(), "UUID生成成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "UUID生成失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void autoGenerateUUIDIfNeeded() {
        // 检查自定义UUID输入框是否为空
        String customUuid = customUuidInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        
        // 如果自定义UUID输入框为空且用户名不为空，则自动生成UUID
        if (customUuid.isEmpty() && !username.isEmpty()) {
            // 生成离线UUID - 使用与shell脚本相同的算法
            String offline = "offline player:" + username;
            String md5 = md5Hash(offline);
            
            // 格式化为UUID格式
            if (md5.length() >= 32) {
                String formattedUUID = String.format("%s-%s-%s-%s-%s",
                    md5.substring(0, 8),
                    md5.substring(8, 12),
                    md5.substring(12, 16),
                    md5.substring(16, 20),
                    md5.substring(20, 32));
                
                uuidDisplay.setText("UUID: " + formattedUUID);
            } else {
                Toast.makeText(requireContext(), "UUID生成失败", Toast.LENGTH_SHORT).show();
            }
        } else if (!customUuid.isEmpty()) {
            // 如果自定义UUID不为空，则显示自定义UUID
            uuidDisplay.setText("UUID: " + customUuid);
        }
    }
    
    private String getUserType() {
        // 获取Spinner中选中的用户类型
        return userTypeSpinner.getSelectedItem().toString();
    }

    private String md5Hash(String input) {
        try {
            android.util.Log.d("QcofA", "计算MD5: " + input);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (Exception e) {
            android.util.Log.e("QcofA", "MD5哈希计算失败", e);
            return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        }
    }

    private void createAccountFile() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        String uuid = extractUUIDFromDisplay();
        if (uuid == null || uuid.isEmpty()) {
            Toast.makeText(requireContext(), "请先生成UUID", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 创建存储目录
            File storageDir = getStorageDir();
            if (!storageDir.exists()) {
                storageDir.mkdirs();
                android.util.Log.d("QcofA", "创建目录: " + storageDir.getAbsolutePath());
            }

            // 创建账号JSON文件
            File jsonFile = new File(storageDir, uuid + ".json");
            JSONObject accountJson = new JSONObject();
            accountJson.put("accessToken", "0");
            accountJson.put("expiresOn", 0);
            accountJson.put("isDemoMode", demoModeCheck.isChecked());
            accountJson.put("userType", getUserType());
            accountJson.put("username", username);
            accountJson.put("uuid", uuid);

            // 写入文件
            FileWriter writer = new FileWriter(jsonFile);
            writer.write(accountJson.toString(2)); // 格式化缩进
            writer.close();

            Toast.makeText(requireContext(), "账号文件创建成功: " + jsonFile.getName(), Toast.LENGTH_LONG).show();
            android.util.Log.d("QcofA", "账号文件创建成功: " + jsonFile.getAbsolutePath());
            
            // 同时更新launcher.conf文件，将新创建的账号添加到配置文件中
            updateLauncherConf(username, uuid);

        } catch (Exception e) {
            android.util.Log.e("QcofA", "创建账号文件失败", e);
            Toast.makeText(requireContext(), "创建账号文件失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateLauncherConf(String username, String uuid) {
        try {
            // 创建launcher.conf文件
            File storageDir = getStorageDir();
            File confFile = new File(storageDir, "launcher.conf");

            JSONObject confJson = new JSONObject();
            confJson.put("acceptedLegal", legalCheck.isChecked());
            confJson.put("setDevMods", devModsCheck.isChecked());
            confJson.put("setCustomRAM", customRamCheck.isChecked());
            confJson.put("customRAMValue", ramValueInput.getText().toString().isEmpty() ? "2048" : ramValueInput.getText().toString());
            confJson.put("lastSelectedInstance", 0);
            confJson.put("lastSelectedAccount", 0);

            // 创建或更新账号数组
            JSONArray accountsArray = new JSONArray();
            
            // 如果配置文件已存在，加载已有账号并添加新账号
            if (confFile.exists()) {
                String existingContent = readFileToString(confFile);
                JSONObject existingConf = new JSONObject(existingContent);
                
                if (existingConf.has("accounts")) {
                    JSONArray existingAccounts = existingConf.getJSONArray("accounts");
                    // 添加已有的账号
                    for (int i = 0; i < existingAccounts.length(); i++) {
                        JSONObject existingAccount = existingAccounts.getJSONObject(i);
                        // 避免重复添加相同UUID的账号
                        if (!existingAccount.getString("uuid").equals(uuid)) {
                            accountsArray.put(existingAccount);
                        }
                    }
                }
            }
            
            // 添加当前新创建的账号
            JSONObject accountObj = new JSONObject();
            accountObj.put("username", username);
            accountObj.put("uuid", uuid);
            accountsArray.put(accountObj);
            
            confJson.put("accounts", accountsArray);

            // 写入文件
            FileWriter writer = new FileWriter(confFile);
            writer.write(confJson.toString(2)); // 格式化缩进
            writer.close();

            Toast.makeText(requireContext(), "配置文件更新成功: " + confFile.getName(), Toast.LENGTH_SHORT).show();
            android.util.Log.d("QcofA", "配置文件更新成功: " + confFile.getAbsolutePath());

        } catch (Exception e) {
            android.util.Log.e("QcofA", "更新配置文件失败", e);
            Toast.makeText(requireContext(), "更新配置文件失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveConfigFiles() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        String uuid = extractUUIDFromDisplay();
        if (uuid == null || uuid.isEmpty()) {
            Toast.makeText(requireContext(), "请先生成UUID", Toast.LENGTH_SHORT).show();
            return;
        }

        updateLauncherConf(username, uuid);
        
        // 保存当前账号信息到SharedPreferences
        saveCurrentAccount(username, uuid);
        
        // 显示当前账号信息
        showCurrentAccountInfo();
    }
    
    private void saveCurrentAccount(String username, String uuid) {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("current_account", android.content.Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", username);
        editor.putString("uuid", uuid);
        editor.apply();
    }
    
    private void loadAndShowCurrentAccount() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("current_account", android.content.Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String uuid = prefs.getString("uuid", "");
        
        if (!username.isEmpty() && !uuid.isEmpty()) {
            usernameInput.setText(username);
            uuidDisplay.setText("UUID: " + uuid);
        }
    }
    
    private void showJreInstallationDialog() {
        // 创建带有三个按钮的对话框 - 使用Material Design
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("手动安装JRE Runtime");
        builder.setMessage("当您启动游戏过程中无法正常下载和安装JRE提供了两个选项可以给您手动下载和安装JRE");

        // 添加"手动下载安装"按钮
        builder.setPositiveButton("手动下载安装", (dialog, which) -> openJreDownloadPage());
        
        // 添加"本地导出JRE"按钮
        builder.setNeutralButton("本地导出JRE", (dialog, which) -> exportJreToPrivateDirectory());
        
        // 添加"取消"按钮
        builder.setNegativeButton("取消", null);
        
        builder.show();
    }
    
    private void openJreDownloadPage() {
        try {
            // 打开浏览器跳转到JRE下载页面
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/QuestCraftPlusPlus/android-openjdk-build-multiarch/releases/tag/jre22-6.0.0"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 需要添加此标志，否则在Fragment中可能会出错
            requireContext().startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "无法打开浏览器: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showAccountsList() {
        try {
            // 读取launcher.conf文件
            File storageDir = getStorageDir();
            File confFile = new File(storageDir, "launcher.conf");
            
            if (!confFile.exists()) {
                Toast.makeText(requireContext(), "暂无已创建的账号", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String content = readFileToString(confFile);
            JSONObject confJson = new JSONObject(content);
            
            JSONArray accountsArray = confJson.getJSONArray("accounts");
            
            if (accountsArray.length() == 0) {
                Toast.makeText(requireContext(), "暂无已创建的账号", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 创建自定义视图的对话框
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setTitle("已创建的账号列表");
            
            // 创建列表视图
            LinearLayout listLayout = new LinearLayout(requireContext());
            listLayout.setOrientation(LinearLayout.VERTICAL);
            listLayout.setPadding(20, 10, 20, 10);
            
            // 为每个账号创建列表项
            for (int i = 0; i < accountsArray.length(); i++) {
                JSONObject account = accountsArray.getJSONObject(i);
                String username = account.getString("username");
                String uuid = account.getString("uuid");
                
                // 创建账号项视图
                View accountItemView = LayoutInflater.from(requireContext()).inflate(R.layout.account_list_item, null);
                
                // 设置账号信息
                TextView usernameView = accountItemView.findViewById(R.id.accountUsername);
                TextView uuidView = accountItemView.findViewById(R.id.accountUuid);
                TextView accountTypeLabel = accountItemView.findViewById(R.id.accountTypeLabel);
                
                usernameView.setText("用户名: " + username);
                uuidView.setText("UUID: " + uuid);
                
                // 获取账户类型，如果没有则默认为离线账户
                String accountType = "offline";
                if (account.has("accountType")) {
                    accountType = account.getString("accountType");
                }
                
                // 根据账户类型设置标签
                if ("premium".equals(accountType)) {
                    accountTypeLabel.setText("正版账户");
                    accountTypeLabel.setBackgroundTintList(getResources().getColorStateList(R.color.state_success));
                } else {
                    accountTypeLabel.setText("离线账户");
                    accountTypeLabel.setBackgroundTintList(getResources().getColorStateList(R.color.state_info));
                }
                
                // 设置标签点击事件
                final int accountIndex = i;
                final String currentAccountType = accountType; // 创建final副本
                accountTypeLabel.setOnClickListener(v -> {
                    try {
                        // 切换账户类型
                        String newAccountType = "offline".equals(currentAccountType) ? "premium" : "offline";
                        
                        // 更新JSON中的账户类型
                        accountsArray.getJSONObject(accountIndex).put("accountType", newAccountType);
                        
                        // 保存更新后的JSON到文件
                        confJson.put("accounts", accountsArray);
                        writeStringToFile(confFile, confJson.toString(2));
                        
                        // 更新标签显示
                        if ("premium".equals(newAccountType)) {
                            accountTypeLabel.setText("正版账户");
                            accountTypeLabel.setBackgroundTintList(getResources().getColorStateList(R.color.state_success));
                        } else {
                            accountTypeLabel.setText("离线账户");
                            accountTypeLabel.setBackgroundTintList(getResources().getColorStateList(R.color.state_info));
                        }
                        
                        Toast.makeText(requireContext(), "账户类型已更新", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        android.util.Log.e("QcofA", "更新账户类型失败", e);
                        Toast.makeText(requireContext(), "更新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                
                listLayout.addView(accountItemView);
                
                // 添加分隔线（除了最后一个）
                if (i < accountsArray.length() - 1) {
                    View divider = new View(requireContext());
                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    divider.setBackgroundColor(getResources().getColor(R.color.outline_variant));
                    listLayout.addView(divider);
                }
            }
            
            // 创建ScrollView包装列表
            ScrollView scrollView = new ScrollView(requireContext());
            scrollView.addView(listLayout);
            
            builder.setView(scrollView);
            builder.setPositiveButton("确定", null);
            builder.show();
                    
        } catch (Exception e) {
            android.util.Log.e("QcofA", "读取账号列表失败", e);
            Toast.makeText(requireContext(), "读取账号列表失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
private void exportJreToPrivateDirectory() {
        try {
            // 获取应用外部存储目录 (/storage/emulated/0/QCOFA.COM/jre_runtime)
            File privateDir = new File(getStorageDir(), "jre_runtime");
            
            if (!privateDir.exists()) {
                privateDir.mkdirs();
            }

            // 定义目标文件路径
            File jreZipFile = new File(privateDir, "JRE.zip");

            // 从assets中读取JRE.zip并写入私有目录
            InputStream inputStream = requireContext().getAssets().open("JRE.zip");
            FileOutputStream outputStream = new FileOutputStream(jreZipFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            Toast.makeText(requireContext(), "JRE已成功导出到目录: " + jreZipFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            android.util.Log.e("QcofA", "导出JRE失败", e);
            Toast.makeText(requireContext(), "导出JRE失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showCurrentAccountInfo() {
        String username = usernameInput.getText().toString().trim();
        String uuid = extractUUIDFromDisplay();
        
        if (!username.isEmpty() && !uuid.isEmpty()) {
            Toast.makeText(requireContext(), "当前账号：\n用户名: " + username + "\nUUID: " + uuid, Toast.LENGTH_LONG).show();
        }
    }

    private void saveVersionListToStorage() {
        try {
            // 获取存储目录 (与账号文件相同目录)
            File storageDir = getStorageDir();
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            // 从assets复制版本列表文件到外部存储
            File destFile = new File(storageDir, "supportedVersions.json");
            InputStream inputStream = requireContext().getAssets().open("supportedVersions.json");
            FileOutputStream outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            Toast.makeText(requireContext(), "版本列表已保存到: " + destFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            android.util.Log.e("QcofA", "保存版本列表失败", e);
            Toast.makeText(requireContext(), "保存版本列表失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showSkinChangeDialog() {
        try {
            // 从assets读取版本列表
            InputStream inputStream = requireContext().getAssets().open("supportedVersions.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(sb.toString());
            JSONArray versions = json.getJSONArray("supportedVersions");

            StringBuilder versionList = new StringBuilder();
            for (int i = 0; i < versions.length(); i++) {
                versionList.append(versions.getString(i)).append("\n");
            }

            // 显示弹窗
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setTitle("当前支持更换皮肤的版本");
            builder.setMessage(versionList.toString().trim());
            builder.setPositiveButton("确定", null);
            builder.show();
        } catch (Exception e) {
            android.util.Log.e("QcofA", "读取版本列表失败", e);
            Toast.makeText(requireContext(), "读取版本列表失败", Toast.LENGTH_SHORT).show();
        }
    }

    private File getStorageDir() {
        // 使用公共存储根目录下的 QCOFA.COM 文件夹，兼容 Pico 等 VR 设备
        File storageDir = new File(Environment.getExternalStorageDirectory(), "QCOFA.COM");
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                // 如果创建失败（可能是权限不足），回退到应用私有目录
                android.util.Log.w("QcofA", "无法在根目录创建 QCOFA.COM 文件夹，回退到应用私有目录");
                File fallbackDir = new File(requireContext().getExternalFilesDir(null), "QCOFA.COM");
                fallbackDir.mkdirs();
                return fallbackDir;
            }
        }
        return storageDir;
    }

    private String extractUUIDFromDisplay() {
        String uuidText = uuidDisplay.getText().toString();
        if (uuidText.startsWith("UUID: ")) {
            return uuidText.substring(6); // 去掉 "UUID: " 前缀
        }
        return null;
    }

    private String readFileToString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(file)));
        String line;
        
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        
        reader.close();
        return sb.toString().trim();
    }

    private void writeStringToFile(File file, String content) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            android.util.Log.e("QcofA", "写入文件失败", e);
            Toast.makeText(requireContext(), "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}