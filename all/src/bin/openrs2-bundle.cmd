@echo off
cd /d %~dp0\..
java -cp lib\openrs2.jar dev.openrs2.bundler.BundlerKt %*
