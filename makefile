compile:
	./gradlew build -x test
	cp app/build/libs/app-all.jar tuber.jar

test:
	DEBUG=true ./gradlew test

clean:
	rm tuber.jar

podcasts-index:
