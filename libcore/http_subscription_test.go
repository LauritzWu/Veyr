package libcore

import (
	"fmt"
	"net"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync/atomic"
	"testing"
)

func TestSubscriptionDirectHTTPS(t *testing.T) {
	var requests atomic.Int32
	server := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		requests.Add(1)
		if r.ProtoMajor != 2 {
			http.Error(w, "HTTP/2 required", http.StatusHTTPVersionNotSupported)
			return
		}
		if r.Header.Get("User-Agent") != "Veyr-Test" || r.URL.Query().Get("token") != "test-share" {
			http.Error(w, "subscription request parameters missing", http.StatusUnauthorized)
			return
		}
		if r.URL.Path == "/unavailable" {
			http.Error(w, "temporarily unavailable", http.StatusServiceUnavailable)
			return
		}
		fmt.Fprint(w, "fixture-subscription")
	}))
	server.EnableHTTP2 = true
	server.StartTLS()
	defer server.Close()

	listener, err := net.Listen("tcp4", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	go func() {
		for {
			conn, acceptErr := listener.Accept()
			if acceptErr != nil {
				return
			}
			conn.Close()
		}
	}()

	client := NewHttpClient().(*httpClient)
	defer client.Close()
	client.tls.RootCAs = server.Client().Transport.(*http.Transport).TLSClientConfig.RootCAs
	client.TrySocks5(int32(listener.Addr().(*net.TCPAddr).Port), "", "")
	client.TryH3Direct()
	client.PreferDirectTLS()

	fetch := func(path string) (HTTPResponse, error) {
		request := client.NewRequest()
		if err := request.SetURL(server.URL + path + "?token=test-share"); err != nil {
			t.Fatal(err)
		}
		request.SetUserAgent("Veyr-Test")
		return request.Execute()
	}

	resp, err := fetch("/subscription")
	if err != nil {
		t.Fatal(err)
	}
	body, err := resp.GetContent()
	if err != nil || string(body) != "fixture-subscription" {
		t.Fatalf("unexpected subscription response: %q, %v", body, err)
	}

	if _, err := fetch("/unavailable"); err == nil || !strings.Contains(err.Error(), "HTTP 503") {
		t.Fatalf("expected HTTP 503, got %v", err)
	}
	if count := requests.Load(); count != 2 {
		t.Fatalf("expected one HTTPS request per refresh, got %d total", count)
	}
}
