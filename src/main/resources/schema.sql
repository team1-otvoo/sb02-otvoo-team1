-- weather_forecasts 테이블
CREATE TABLE weather_forecasts (
                                   id UUID PRIMARY KEY,
                                   forecasted_at TIMESTAMP NOT NULL,
                                   forecast_at TIMESTAMP NOT NULL,
                                   sky_status VARCHAR(30) NOT NULL,
                                   created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                   updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- weather_locations 테이블
CREATE TABLE weather_locations (
                                   id UUID PRIMARY KEY,
                                   forecast_id UUID NOT NULL,
                                   latitude DOUBLE PRECISION NOT NULL,
                                   longitude DOUBLE PRECISION NOT NULL,
                                   x INT NOT NULL,
                                   y INT NOT NULL,
                                   location_names TEXT,
                                   CONSTRAINT fk_weather_locations_forecast FOREIGN KEY (forecast_id)
                                       REFERENCES weather_forecasts(id) ON DELETE CASCADE
);

-- users 테이블
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(100) NOT NULL,
                       role VARCHAR(50) NOT NULL,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                       follower_count BIGINT NOT NULL DEFAULT 0,
                       following_count BIGINT NOT NULL DEFAULT 0,
                       locked BOOLEAN NOT NULL DEFAULT FALSE
);

-- profiles 테이블
CREATE TABLE profiles (
                          id UUID PRIMARY KEY,
                          name VARCHAR(50) NOT NULL,
                          gender VARCHAR(10),
                          birth DATE,
                          temperature_sensitivity INT,
                          user_id UUID NOT NULL UNIQUE,
                          weather_location_id UUID,

                          CONSTRAINT fk_profiles_user FOREIGN KEY (user_id)
                              REFERENCES users(id) ON DELETE CASCADE,
                          CONSTRAINT fk_profiles_weather_locations FOREIGN KEY (weather_location_id)
                              REFERENCES weather_locations(id) ON DELETE SET NULL
);

-- profile_images 테이블
CREATE TABLE profile_images (
                                id UUID PRIMARY KEY,
                                image_url TEXT,
                                original_filename VARCHAR(255),
                                content_type VARCHAR(50),
                                size BIGINT,
                                width INT,
                                height INT,
                                uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
                                profile_id UUID NOT NULL UNIQUE,

                                CONSTRAINT fk_profiles_images_profile FOREIGN KEY (profile_id)
                                    REFERENCES profiles(id) ON DELETE CASCADE
);

-- clothes_attribute_definitions 테이블
CREATE TABLE clothes_attribute_definitions (
                                               id UUID PRIMARY KEY,
                                               name VARCHAR(255) NOT NULL UNIQUE,
                                               created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- clothes_attribute_values 테이블
CREATE TABLE clothes_attribute_values (
                                          id UUID PRIMARY KEY,
                                          definition_id UUID NOT NULL,
                                          value VARCHAR(255) NOT NULL,
                                          order_index INT NOT NULL DEFAULT 0,
                                          CONSTRAINT fk_clothes_attribute_definition FOREIGN KEY (definition_id)
                                              REFERENCES clothes_attribute_definitions(id) ON DELETE CASCADE,
                                          CONSTRAINT uq_definition_value UNIQUE (definition_id, value)
);

-- weather_temperatures 테이블
CREATE TABLE weather_temperatures (
                                      forecast_id UUID PRIMARY KEY,
                                      current DOUBLE PRECISION NOT NULL,
                                      min DOUBLE PRECISION,
                                      max DOUBLE PRECISION,
                                      compared_to_day_before DOUBLE PRECISION NOT NULL,
                                      CONSTRAINT fk_weather_temperatures_forecast FOREIGN KEY (forecast_id)
                                          REFERENCES weather_forecasts(id) ON DELETE CASCADE
);

-- weather_precipitations 테이블
CREATE TABLE weather_precipitations (
                                        forecast_id UUID PRIMARY KEY,
                                        type VARCHAR(20) NOT NULL,
                                        amount DOUBLE PRECISION,
                                        probability DOUBLE PRECISION NOT NULL,
                                        CONSTRAINT fk_weather_precipitations_forecast FOREIGN KEY (forecast_id)
                                            REFERENCES weather_forecasts(id) ON DELETE CASCADE
);

-- weather_humidities 테이블
CREATE TABLE weather_humidities (
                                    forecast_id UUID PRIMARY KEY,
                                    current DOUBLE PRECISION NOT NULL,
                                    compared_to_day_before DOUBLE PRECISION NOT NULL,
                                    CONSTRAINT fk_weather_humidities_forecast FOREIGN KEY (forecast_id)
                                        REFERENCES weather_forecasts(id) ON DELETE CASCADE
);

-- weather_wind_speeds 테이블
CREATE TABLE weather_wind_speeds (
                                     forecast_id UUID PRIMARY KEY,
                                     speed DOUBLE PRECISION NOT NULL,
                                     as_word VARCHAR(20) NOT NULL,
                                     CONSTRAINT fk_weather_wind_speeds_forecast FOREIGN KEY (forecast_id)
                                         REFERENCES weather_forecasts(id) ON DELETE CASCADE
);

-- clothes 테이블
CREATE TABLE clothes (
                         id UUID PRIMARY KEY,
                         owner_id UUID NOT NULL,
                         name VARCHAR(255) NOT NULL,
                         type VARCHAR(50) NOT NULL,
                         image_url TEXT,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         CONSTRAINT fk_clothes_owner FOREIGN KEY (owner_id)
                             REFERENCES users(id) ON DELETE CASCADE
);

-- clothes_selected_values 테이블
CREATE TABLE clothes_selected_values (
                                         id UUID PRIMARY KEY,
                                         clothes_id UUID NOT NULL,
                                         definition_id UUID NOT NULL,
                                         value_id UUID NOT NULL,
                                         CONSTRAINT fk_clothes_selected_values_clothes FOREIGN KEY (clothes_id)
                                             REFERENCES clothes(id) ON DELETE CASCADE,
                                         CONSTRAINT fk_clothes_selected_values_definition FOREIGN KEY (definition_id)
                                             REFERENCES clothes_attribute_definitions(id) ON DELETE CASCADE,
                                         CONSTRAINT fk_clothes_selected_values_value FOREIGN KEY (value_id)
                                             REFERENCES clothes_attribute_values(id) ON DELETE CASCADE
);

-- recommendations 테이블
CREATE TABLE recommendations (
                                 id UUID PRIMARY KEY,
                                 user_id UUID NOT NULL,
                                 weather_id UUID NOT NULL,
                                 created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                 CONSTRAINT fk_recommendations_users FOREIGN KEY (user_id)
                                     REFERENCES users(id) ON DELETE CASCADE,
                                 CONSTRAINT fk_recommendations_forecast FOREIGN KEY (weather_id)
                                     REFERENCES weather_forecasts(id) ON DELETE CASCADE
);

-- recommendation_clothes 테이블
CREATE TABLE recommendation_clothes (
                                        id                UUID PRIMARY KEY,
                                        recommendation_id UUID NOT NULL,
                                        clothes_id        UUID NOT NULL,
                                        clothes_order     INT  NOT NULL,

                                        CONSTRAINT fk_recommendation FOREIGN KEY (recommendation_id)
                                            REFERENCES recommendations(id) ON DELETE CASCADE,
                                        CONSTRAINT fk_clothes FOREIGN KEY (clothes_id)
                                            REFERENCES clothes(id) ON DELETE CASCADE
);

-- feeds 테이블
CREATE TABLE feeds (
                       id UUID PRIMARY KEY,
                       user_id UUID,
                       forecast_id UUID,
                       content TEXT,
                       like_count BIGINT NOT NULL DEFAULT 0,
                       comment_count BIGINT NOT NULL DEFAULT 0,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ,
                       is_deleted BOOLEAN DEFAULT FALSE,
                       CONSTRAINT fk_feed_user FOREIGN KEY (user_id)
                           REFERENCES users(id) ON DELETE SET NULL,
                       CONSTRAINT fk_feed_forecast FOREIGN KEY (forecast_id)
                           REFERENCES weather_forecasts(id) ON DELETE SET NULL
);

-- feed_clothes 테이블
CREATE TABLE feed_clothes (
                              id UUID PRIMARY KEY,
                              feed_id UUID NOT NULL,
                              clothes_id UUID NOT NULL,
                              CONSTRAINT fk_feed_clothes_feed FOREIGN KEY (feed_id)
                                  REFERENCES feeds(id) ON DELETE CASCADE,
                              CONSTRAINT fk_feed_clothes_recommendation FOREIGN KEY (clothes_id)
                                  REFERENCES clothes(id) ON DELETE CASCADE
);

-- feed_comments 테이블
CREATE TABLE feed_comments (
                               id UUID PRIMARY KEY,
                               user_id UUID,
                               feed_id UUID NOT NULL,
                               content TEXT,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               is_deleted BOOLEAN DEFAULT FALSE,
                               CONSTRAINT fk_feedcomment_user FOREIGN KEY (user_id)
                                   REFERENCES users(id) ON DELETE SET NULL,
                               CONSTRAINT fk_feedcomment_feed FOREIGN KEY (feed_id)
                                   REFERENCES feeds(id) ON DELETE CASCADE
);

-- feed_likes 테이블
CREATE TABLE feed_likes (
                            id UUID PRIMARY KEY,
                            liked_by UUID NOT NULL,
                            feed_id UUID NOT NULL,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            CONSTRAINT fk_feedlike_user FOREIGN KEY (liked_by)
                                REFERENCES users(id) ON DELETE CASCADE,
                            CONSTRAINT fk_feedlike_feed FOREIGN KEY (feed_id)
                                REFERENCES feeds(id) ON DELETE CASCADE
);

-- follows 테이블
CREATE TABLE follows (
                         id UUID PRIMARY KEY,
                         followee_id UUID NOT NULL,
                         follower_id UUID NOT NULL,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         CONSTRAINT uq_followee_follower UNIQUE (followee_id, follower_id),
                         CONSTRAINT fk_followee FOREIGN KEY (followee_id)
                             REFERENCES users(id) ON DELETE CASCADE,
                         CONSTRAINT fk_follower FOREIGN KEY (follower_id)
                             REFERENCES users(id) ON DELETE CASCADE
);

-- direct_messages 테이블
CREATE TABLE direct_messages (
                                 id UUID PRIMARY KEY,
                                 sender_id UUID NOT NULL,
                                 receiver_id UUID NOT NULL,
                                 content TEXT NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 CONSTRAINT fk_sender FOREIGN KEY (sender_id)
                                     REFERENCES users(id),
                                 CONSTRAINT fk_receiver FOREIGN KEY (receiver_id)
                                     REFERENCES users(id)
);

-- notifications 테이블
CREATE TABLE notifications (
                               id UUID PRIMARY KEY,
                               receiver_id UUID NOT NULL,
                               title VARCHAR(255) NOT NULL,
                               content VARCHAR(255) NOT NULL,
                               level VARCHAR(20) NOT NULL,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               CONSTRAINT fk_receiver FOREIGN KEY (receiver_id)
                                   REFERENCES users(id) ON DELETE CASCADE
);
