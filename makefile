compile:
	./gradlew build -x test
	# cp app/build/libs/app-all.jar tuber.jar
	cp app/build/distributions/app.zip tuber.zip

test:
	DEBUG=true ./gradlew test

clean:
	rm tuber.zip

release:
