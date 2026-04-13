#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const platforms = [
  {
    name: 'Linux x64',
    source: 'node_modules/@github/copilot-language-server-linux-x64/copilot-language-server',
    target: '../../com.microsoft.copilot.eclipse.core.agent.linux.x64/copilot-agent/copilot-language-server'
  },
  {
    name: 'Linux ARM64',
    source: 'node_modules/@github/copilot-language-server-linux-arm64/copilot-language-server',
    target: '../../com.microsoft.copilot.eclipse.core.agent.linux.aarch64/copilot-agent/copilot-language-server'
  },
  {
    name: 'Windows x64',
    source: 'node_modules/@github/copilot-language-server-win32-x64/copilot-language-server.exe',
    target: '../../com.microsoft.copilot.eclipse.core.agent.win32/copilot-agent/copilot-language-server.exe'
  },
  {
    name: 'macOS x64',
    source: 'node_modules/@github/copilot-language-server-darwin-x64/copilot-language-server',
    target: '../../com.microsoft.copilot.eclipse.core.agent.macosx.x64/copilot-agent/copilot-language-server'
  },
  {
    name: 'macOS ARM64',
    source: 'node_modules/@github/copilot-language-server-darwin-arm64/copilot-language-server',
    target: '../../com.microsoft.copilot.eclipse.core.agent.macosx.aarch64/copilot-agent/copilot-language-server'
  }
];

function ensureDirectoryExists(filePath) {
  const dir = path.dirname(filePath);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

function copyFile(source, target) {
  if (!fs.existsSync(source)) {
    throw new Error(`Source file does not exist: ${source}`);
  }
  
  ensureDirectoryExists(target);
  fs.copyFileSync(source, target);
  
  // Set executable permissions on Unix systems
  if (process.platform !== 'win32' && !target.endsWith('.exe')) {
    fs.chmodSync(target, 0o755);
  }
}

function copyDistFiles() {
  try {
    execSync('npx --yes copyfiles -u 3 "node_modules/@github/copilot-language-server/dist/**/*" "."', { stdio: 'inherit' });
  } catch (error) {
    throw new Error(`Failed to copy dist files: ${error.message}`);
  }
}

function main() {
  try {
    copyDistFiles();
  } catch (error) {
    console.error(error.message);
    process.exit(1);
  }
  
  for (const platform of platforms) {
    try {
      copyFile(platform.source, platform.target);
    } catch (error) {
      console.error(`Failed to copy ${platform.name} distribution:`, error.message);
      process.exit(1);
    }
  }
}

main();
