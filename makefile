compile:
	./gradlew build -x test
	# cp app/build/libs/app-all.jar tuber.jar
	cp app/build/distributions/app.zip tuber.zip

test:
	DEBUG=true ./gradlew test

clean:
	rm tuber.zip

get-auto-release-notes:
	gh api repos/:owner/:repo/compare/$$(gh release view --json tagName -q .tagName)...HEAD --jq '.commits[].commit.message'
